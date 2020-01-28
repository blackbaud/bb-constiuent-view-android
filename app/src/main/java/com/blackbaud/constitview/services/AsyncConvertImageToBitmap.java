package com.blackbaud.constitview.services;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@SuppressLint("StaticFieldLeak")
public class AsyncConvertImageToBitmap extends AsyncTask<String, String, Bitmap> {

    @Override
    protected Bitmap doInBackground(String... strings) {
        try {
            // Get image URL
            JSONObject json = new JSONObject(strings[0]);
            return BitmapFactory.decodeStream((InputStream)new URL(json.getString("url")).getContent());
        } catch (JSONException | IOException e) {
            // Handle error
        }
        return null;
    }
}
