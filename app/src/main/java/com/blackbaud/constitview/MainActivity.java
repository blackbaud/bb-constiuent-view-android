package com.blackbaud.constitview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        if (intent != null) {
            goToLogin();
        }
        // intent was null for some reason render the page like normal
        else {
            Button loginButton = findViewById((R.id.loginButton));
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToLogin();
                }
            });
        }
    }


    //TODO: will use this to handle voice parameters later
    private void handleIntent(Intent intent){
        if (intent != null){
            String action = intent.getAction();
            // request from assistant
            if (Intent.ACTION_SEND.equals(action)){
                goToLogin();
            }
            // request from normal application run
            else{
                Button loginButton = findViewById((R.id.loginButton));
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goToLogin();
                    }
                });
            }
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    private void goToConstitRecord() {
        Intent intent = new Intent(this, ConstitRecord.class);
        startActivity(intent);
    }
}