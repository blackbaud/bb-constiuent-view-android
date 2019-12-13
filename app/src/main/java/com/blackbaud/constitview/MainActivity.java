package com.blackbaud.constitview;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.slice.SliceManager;


public class MainActivity extends AppCompatActivity {

    private static final String SLICE_AUTHORITY = "com.blackbaud.constitview";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        grantSlicePermissions();

        Intent intent = getIntent();

        if (intent != null) {
            handleIntent(intent);
        }
        // intent was null for some reason render the page like normal
        else {
            Button loginButton = findViewById((R.id.loginButton));
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToLogin(intent);
                }
            });
        }
    }


    //TODO: will use this to handle voice parameters later
    private void handleIntent(Intent intent){
        if (intent != null){
            String action = intent.getAction();
            // request from assistant
            if (Intent.ACTION_VIEW.equals(action)){
                goToLogin(intent);
            }
            // request from normal application run
            else{
                Button loginButton = findViewById((R.id.loginButton));
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goToLogin(intent);
                    }
                });
            }
        }
    }

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