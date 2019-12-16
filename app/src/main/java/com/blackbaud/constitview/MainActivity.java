package com.blackbaud.constitview;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.slice.SliceManager;


public class MainActivity extends AppCompatActivity {

    private static final String SLICE_AUTHORITY = "com.blackbaud.constitview";
    private SharedPreferences sharedPreferences;

    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        grantSlicePermissions();

        loginButton = findViewById((R.id.loginButton));

        sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }
        // intent was null for some reason render the page like normal
        else {
            loginButton.setOnClickListener(v -> goToLogin(getIntent()));
        }
    }

    @SuppressLint("ApplySharedPref")
    private void handleIntent(Intent intent){
        if (intent != null){
            String action = intent.getAction();
            // request from assistant
            if (Intent.ACTION_VIEW.equals(action)){
                goToLogin(intent);
            }
            // request from normal application run
            else{
                boolean loggedIn = !sharedPreferences.getBoolean("expired", false);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (loggedIn){
                    loginButton.setText(R.string.logout_button);
                    loginButton.setOnClickListener(v -> {
                        Date now = new Date();
                        editor.putString("expires", now.toString());
                        editor.putBoolean("expired", true);
                        editor.commit();
                        loginButton.setText(R.string.login_button);
                        refreshActivity();
                    });
                } else {
                    loginButton.setOnClickListener(v -> {
                        editor.putString("featureName", null);
                        goToLogin(intent);
                    });
                }
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void goToLogin(Intent requestIntent) {
        Intent intent = new Intent(this, Login.class);

        if (requestIntent != null) {
            String name = parseNameFromIntent(requestIntent);
            SharedPreferences sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("featureName", name);
            editor.commit();
        }
        startActivity(intent);
    }

    private void refreshActivity(){
        finish();
        startActivity(getIntent());
    }

    private String parseNameFromIntent(Intent intent) {
        Uri data = intent.getData();
        String name = "";
        if (data != null) {
            String[] response = data.toString().split("\\?");
            String responseParam = null;
            if (response.length > 1){
                responseParam = response[1].replace("%20", " ");
            }
            try {
                JSONObject json = new JSONObject(paramJson(responseParam));
                name = json.getString("featureName");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return name;
    }

    // Parse the response from the auth SPA and turn it into a json style string
    public static String paramJson(String paramIn) {
        paramIn = paramIn.replaceAll("=", "\":\"");
        paramIn = paramIn.replaceAll("&", "\",\"");
        return "{\"" + paramIn + "\"}";
    }

    private void grantSlicePermissions() {
        Context context = getApplicationContext();
        Uri sliceProviderUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(SLICE_AUTHORITY)
                        .build();

        String assistantPackage = getAssistantPackage(context);
        if (assistantPackage == null) {
            return;
        }
        SliceManager.getInstance(context)
                .grantSlicePermission(assistantPackage, sliceProviderUri);
    }

    private String getAssistantPackage(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentServices(
                new Intent(VoiceInteractionService.SERVICE_INTERFACE), 0);
        if (resolveInfoList.isEmpty()) {
            return null;
        }
        return resolveInfoList.get(0).serviceInfo.packageName;
    }
}