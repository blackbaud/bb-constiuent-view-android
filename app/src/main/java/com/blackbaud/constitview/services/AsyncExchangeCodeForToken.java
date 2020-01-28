package com.blackbaud.constitview.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.blackbaud.constitview.models.ExchangeCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Async class used to exchange a code from logging in for access token credentials.
// This should only happen upon first login or whenever the user logs out.
@SuppressLint("StaticFieldLeak")
public class AsyncExchangeCodeForToken extends AsyncTask<ExchangeCode, String, String> {

    // The string representation of the json response from the token exchange
    private String responseText = null;
    private SharedPreferences sharedPreferences;

    @Override
    protected String doInBackground(ExchangeCode... params) {
        Context context = params[0].getContext();
        String code = params[0].getCode();

        sharedPreferences = context.getSharedPreferences("TokenCache", Context.MODE_PRIVATE);

        try {
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, "grant_type=authorization_code&redirect_uri=https%3A%2F%2Fhost.nxt.blackbaud.com%2Fapp-redirect%2Fredirect-androiddemo%2F&code=" + code);
            Request request = new Request.Builder()
                    .url("https://oauth2.sky.blackbaud.com/token")
                    .post(body)
                    .addHeader("Authorization", "Basic Mzg1Zjg5NWYtYjI4NC00ZGM5LWFmOGEtMGMxMjYwYjNlM2YyOlhZL2VWbVlSMG0wdEwvTUZ3QWJjL0dubi91MVN0bEIxSHB3SXFCb3o0TDg9")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            Response response = client.newCall(request).execute();
            assert response.body() != null;
            responseText = response.body().string();
        } catch (IOException ex) {
            // Handle ex
        }

        return responseText;
    }

    @Override
    @SuppressLint("SimpleDateFormat")
    protected void onPostExecute(String result) {
        super.onPreExecute();
        if (responseText != null) {
            JSONObject json;
            try {
                json = new JSONObject(responseText);
                if (json.length() > 0) {
                    try {
                        // SharedPreferences sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = this.sharedPreferences.edit();

                        // Set the current date and time plus 58 minutes as the access token's expiration
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm");
                        String currentDateAndTime = sdf.format(new Date());
                        Date date = sdf.parse(currentDateAndTime);
                        Calendar calendar = Calendar.getInstance();
                        assert date != null;
                        calendar.setTime(date);
                        calendar.add(Calendar.MINUTE, 58);

                        // Store refresh token expiration date
                        // editor.putString("refreshTokenExpiration", refreshTokenExipration);

                        // Get date response into a Java parsable date string and store
                        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy:MM:dd");
                        String accessTokenExpDate = dateSdf.format(calendar.getTime());

                        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss");
                        String accessTokenExpTime = timeSdf.format(calendar.getTime());
                        String expires = accessTokenExpDate + " " + accessTokenExpTime;
                        editor.putString("expires", expires);

                        // Store current token
                        String bearerToken = "Bearer " + json.getString("access_token");
                        editor.putString("bearerToken", bearerToken);
                        editor.commit();
                    } catch (JSONException | ParseException e) {
                        // Handle error
                    }
                }
            } catch (JSONException e) {
                // Handle error
            }
        }
    }
}
