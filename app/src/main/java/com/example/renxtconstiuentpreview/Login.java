package com.example.renxtconstiuentpreview;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private String url = "https://oauth2.sky.blackbaud.com/authorization?client_id=385f895f-b284-4dc9-af8a-0c1260b3e3f2&response_type=code&redirect_uri=https://host.nxt.blackbaud.com/app-redirect/redirect-stevenandroidassistant/";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView = findViewById(R.id.webView);
        webView.loadUrl(url);
        webView.getSettings().setJavaScriptEnabled(true);
    }
}
