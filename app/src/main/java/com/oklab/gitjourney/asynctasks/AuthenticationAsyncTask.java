package com.oklab.gitjourney.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.activities.MainActivity;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Created by olgakuklina on 2017-01-08.
 */

public class AuthenticationAsyncTask extends AsyncTask<String, Integer, UserSessionData> {

    private static final String TAG = AuthenticationAsyncTask.class.getSimpleName();
    private final Context context;

    public AuthenticationAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected UserSessionData doInBackground(String... args) {
        try {
            HttpURLConnection connect = (HttpURLConnection) new URL(context.getString(R.string.url_connect)).openConnection();
            connect.setRequestMethod("POST");
            connect.setDoOutput(true);
            String inputString = args[0] + ":" + args[1];
            String credentials = Base64.encodeToString(inputString.getBytes(), Base64.NO_WRAP);
            String authentication = "basic " + credentials;
            connect.setRequestProperty("Authorization", authentication);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("client_id", context.getString(R.string.client_id));
            jsonObject.put("client_secret", context.getString(R.string.client_secret));
            jsonObject.put("note", context.getString(R.string.note));
            JSONArray jsonArray = new JSONArray();
            jsonArray.put("repo");
            jsonArray.put("user");
            jsonObject.put("scopes", jsonArray);

            OutputStream outputStream = connect.getOutputStream();
            Log.v(TAG, "request body = " + jsonObject.toString());
            outputStream.write(jsonObject.toString().getBytes());
            connect.connect();
            int responseCode = connect.getResponseCode();

            Log.v(TAG, "responseCode = " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                return null;
            }
            InputStream inputStream = connect.getInputStream();
            String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            JSONObject jObj = new JSONObject(response);

            HttpURLConnection reconnect = (HttpURLConnection) new URL(context.getString(R.string.url_login_data)).openConnection();
            reconnect.setRequestMethod("GET");

            authentication = "token " + jObj.getString("token");
            reconnect.setRequestProperty("Authorization", authentication);

            reconnect.connect();
            responseCode = connect.getResponseCode();
            Log.v(TAG, "responseCode = " + responseCode);

            inputStream = reconnect.getInputStream();
            response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            JSONObject object = new JSONObject(response);
            UserSessionData data = new UserSessionData(jObj.getString("id"), credentials, jObj.getString("token"), object.getString("login"));
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Login failed", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(UserSessionData userSessionData) {
        super.onPostExecute(userSessionData);
        if (userSessionData != null) {
            SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
            SharedPreferences.Editor e = prefs.edit();
            e.putString("userSessionData", userSessionData.toString()); // save "value" to the SharedPreferences
            e.apply();
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.getString(R.string.login_failed), LENGTH_SHORT).show();
        }
    }
}
