package com.blackbaud.constitview.services;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.blackbaud.constitview.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressLint("StaticFieldLeak")
public class AsyncGet extends AsyncTask<String, String, String> {

    private String responseText = null;

    @Override
    protected String doInBackground(String... strings) {
        String reqUrl = strings[0];
        String bearerToken = strings[1];
        String subscriptionKey = strings[2];

        try {
            URL url = new URL(reqUrl);
            OkHttpClient client = new OkHttpClient();

           Request request = new Request.Builder()
                   .url(url)
                   .get()
                   .addHeader("Authorization", bearerToken)
                   .addHeader("Bb-Api-Subscription-Key", subscriptionKey)
                   .build();

            Response response = client.newCall(request).execute();
            assert response.body() != null;
            responseText = response.body().string();
        } catch (IOException ex) {
            // Handle ex
        }

        return responseText;
    }
}