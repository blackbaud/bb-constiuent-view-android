package com.blackbaud.constitview.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.blackbaud.constitview.R;
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
        ExchangeCode exchangeCode = params[0];
        String applicationAuthorization  = "Basic " + exchangeCode.getContext().getResources().getString(R.string.applicationAuthorization);

        sharedPreferences = exchangeCode.getContext().getSharedPreferences("TokenCache", Context.MODE_PRIVATE);

        try {
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            Response response = null;

            // Exchange auth code for access token
            if (exchangeCode.getRefreshToken() == null) {
                RequestBody body = RequestBody.create(
                        mediaType,
                        "grant_type=" + exchangeCode.getGrantType()  + "&redirect_uri=" + exchangeCode.getRedirectUri() + "&code=" + exchangeCode.getCode());
                Request request = new Request.Builder()
                        .url("https://oauth2.sky.blackbaud.com/token")
                        .post(body)
                        .addHeader("Authorization", applicationAuthorization)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                response = client.newCall(request).execute();
            }

            // Exchange refresh token for access token
            if (exchangeCode.getRefreshToken() != null) {
                RequestBody body = RequestBody.create(mediaType, "grant_type=" + exchangeCode.getGrantType()  + "&refresh_token=" + exchangeCode.getRefreshToken());
                Request request = new Request.Builder()
                        .url("https://oauth2.sky.blackbaud.com/token")
                        .post(body)
                        .addHeader("Authorization", applicationAuthorization)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                response = client.newCall(request).execute();
            }

            if (response.body() != null) {
                responseText = response.body().string();
            }
        } catch (IOException ex) {
            // Handle ex
        }

        return responseText;
    }

    // Update all of the cached keys and expiration date/times
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
                        calendar.add(Calendar.MINUTE, Integer.parseInt(json.getString("expires_in"))/60);

                        // Store refresh token expiration date
                        // editor.putString("refreshTokenExpiration", refreshTokenExipration);

                        // Get date response into a Java parsable date string and store
                        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy:MM:dd");
                        String accessTokenExpDate = dateSdf.format(calendar.getTime());

                        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss");
                        String accessTokenExpTime = timeSdf.format(calendar.getTime());
                        String expires = accessTokenExpDate + " " + accessTokenExpTime;
                        editor.putString("expires", expires);

                        // Store current access token
                        String bearerToken = "Bearer " + json.getString("access_token");
                        editor.putString("bearerToken", bearerToken);

                        // Store current refresh token
                        String refreshToken = json.getString("refresh_token");
                        editor.putString("refreshToken", refreshToken);

                        // Store refresh token expiration
                        Calendar refreshTokenCalender = Calendar.getInstance();
                        refreshTokenCalender.setTime(date);
                        refreshTokenCalender.add(Calendar.MINUTE, Integer.parseInt(json.getString("refresh_token_expires_in")));
                        String refreshTokenExpDate = dateSdf.format(refreshTokenCalender.getTime());
                        String refreshTokenExpTime = timeSdf.format(refreshTokenCalender.getTime());
                        String refreshTokenExpires = refreshTokenExpDate + " " + refreshTokenExpTime;
                        editor.putString("refreshTokenExpires", refreshTokenExpires);
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

