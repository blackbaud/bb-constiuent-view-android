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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
        String bearerToken = sharedPreferences.getString("bearerToken", null);
        Boolean tokenExpired = checkIfTokenExpired();

        // token is still good load up constit record activity
        if (bearerToken != null && !tokenExpired){
            goToConstitRecord();
        }

        // token never existed or expired so initiate login sequence then go to constit activity
        else {
            WebView webView = findViewById(R.id.webView);
            String url = "https://oauth2.sky.blackbaud.com/authorization?client_id=385f895f-b284-4dc9-af8a-0c1260b3e3f2&response_type=code&redirect_uri=https://host.nxt.blackbaud.com/app-redirect/redirect-stevenandroidassistant/";
            webView.loadUrl(url);
            webView.getSettings().setJavaScriptEnabled(true);
        }
    }

    private Boolean checkIfTokenExpired() {
        String expireDateTime = sharedPreferences.getString("expires", null);
        if (expireDateTime != null) {
            SimpleDateFormat tokenDateFormatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.US);
            try {
                Date tokenDate = tokenDateFormatter.parse(expireDateTime);
                Date now = new Date();
                if (tokenDate != null && now.after(tokenDate)){
                    SharedPreferences.Editor editor = sharedPreferences.edit();
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
