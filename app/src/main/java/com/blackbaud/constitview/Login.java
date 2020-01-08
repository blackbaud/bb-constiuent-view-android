package com.blackbaud.constitview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private String authUrl = "https://oauth2.sky.blackbaud.com/authorization?";

    @SuppressLint({"SetJavaScriptEnabled", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get your client Id and redirect URI from the string resource file
        String clientId = getResources().getString(R.string.clientId);
        String redirectUri = getResources().getString(R.string.redirectUri);

        sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
        String bearerToken = sharedPreferences.getString("bearerToken", null);
        Boolean tokenExpired = sharedPreferences.getBoolean("expired", false);

        if(!tokenExpired) {
             tokenExpired = checkIfTokenExpired();
        }

        // token is still good load up constit record activity
        if (bearerToken != null && !tokenExpired){
            goToConstitRecord();
        }

        // token never existed or expired so initiate login sequence then go to constit activity
        else {
            authUrl += String.format("client_id=%s&response_type=code&redirect_uri=%s", clientId, redirectUri);
            editor = sharedPreferences.edit();
            editor.putBoolean("expired", false);
            editor.commit();
            WebView webView = findViewById(R.id.webView);
            webView.loadUrl(authUrl);
            webView.getSettings().setJavaScriptEnabled(true);
        }
    }

    @SuppressLint("ApplySharedPref")
    private Boolean checkIfTokenExpired() {
        String expireDateTime = sharedPreferences.getString("expires", null);
        if (expireDateTime != null) {
            SimpleDateFormat tokenDateFormatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.US);
            try {
                Date tokenDate = tokenDateFormatter.parse(expireDateTime);
                Date now = new Date();
                if (tokenDate != null && now.after(tokenDate)){
                    editor = sharedPreferences.edit();
                    editor.putBoolean("expired", true);
                    editor.commit();
                    return true;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            return true;
        }
    }

    private void goToConstitRecord() {
        Intent intent = new Intent(this, ConstitRecord.class);
        startActivity(intent);
    }
}
