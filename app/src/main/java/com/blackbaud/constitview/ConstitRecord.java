package com.blackbaud.constitview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.blackbaud.constitview.models.ExchangeCode;
import com.blackbaud.constitview.services.AsyncConvertImageToBitmap;
import com.blackbaud.constitview.services.AsyncExchangeCodeForToken;
import com.blackbaud.constitview.services.AsyncSkyApiGet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;
import info.androidhive.fontawesome.FontTextView;

public class ConstitRecord extends AppCompatActivity {

    private String skyApiUrl = "https://api.sky.blackbaud.com/constituent/v1/constituents/";
    private String subscriptionKey;

    private String bearerToken = null;

    // Debug log tag.
    private static final String TAG_HTTP_URL_CONNECTION = "HTTP_URL_CONNECTION";

    // Child thread sent message type value to activity main thread Handler.
    private static final int REQUEST_CODE_SHOW_RESPONSE_TEXT = 1;

    // The key of message stored server returned data.
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";

    private ImageView profileImage;
    private TextView nameText;
    private TextView addressText;
    private TextView phoneText;
    private FontTextView addressPinIcon;
    private FontTextView phoneIcon;

    private SharedPreferences constitRecordCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constit_record);

        // Get your SKY Api subscription key from the string resource file
        subscriptionKey = getResources().getString(R.string.subscriptionKey);

        profileImage = findViewById(R.id.profileImage);
        nameText = findViewById(R.id.constitNameText);
        addressText = findViewById(R.id.constitAddressText);
        phoneText = findViewById(R.id.constitPhoneText);
        addressPinIcon = findViewById(R.id.mapIcon);
        phoneIcon = findViewById(R.id.phoneIcon);

        SharedPreferences sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
        constitRecordCache = getSharedPreferences("ConstitRecordCache", Context.MODE_PRIVATE);
        bearerToken = sharedPreferences.getString("bearerToken", null);

        // Set the cached token info
        handleIntent(getIntent());

        String constitName = sharedPreferences.getString("featureName", null);

        if (constitName != null && !constitName.equals("")) {
            String constitId = getConstitId(constitName);
            if (constitId == null) {
                nameText.setText(R.string.record_not_found);
            }
            updateRecordImage(constitId);
            updateRecordText(constitId);
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    // Get an access token from SKY API
    private void handleIntent(Intent intent) {
        if (intent != null){
            String action = intent.getAction();
            if (action != null){
                Uri data = intent.getData();
                if (data != null) {
                    String response = data.toString().split("android-deeplink\\?")[1];
                    try {
                        // Create a json object out of the response from the auth SPA
                        JSONObject json = new JSONObject(paramJson(response));
                        String accessCode = json.getString("code");

                        ExchangeCode exchangeCode = new ExchangeCode(this.getApplicationContext(), accessCode, null, "authorization_code", "https%3A%2F%2Fhost.nxt.blackbaud.com%2Fapp-redirect%2Fredirect-androiddemo%2F&code=");

                        // Set cashed token data using returned code
                        AsyncExchangeCodeForToken asyncExchangeCodeForToken = new AsyncExchangeCodeForToken();
                        asyncExchangeCodeForToken.execute(exchangeCode);
                    } catch (JSONException e) {
                        // Handle error
                    }
                }
            }
        }
    }

    // Parse the response from the auth SPA and turn it into a json style string
    private static String paramJson(String paramIn) {
        paramIn = paramIn.replaceAll("=", "\":\"");
        paramIn = paramIn.replaceAll("&", "\",\"");
        return "{\"" + paramIn + "\"}";
    }

    private void updateRecordImage(String constitId) {
        AsyncSkyApiGet asyncUpdateConstitImage = new AsyncSkyApiGet();
        String responseText = null;
        try {
            responseText = asyncUpdateConstitImage.execute(skyApiUrl + constitId + "/profilepicture", bearerToken, subscriptionKey).get();
        } catch (ExecutionException | InterruptedException e) {
            // Handle error
        }
        if (responseText != null) {
            JSONObject json = null;
            try {
                json = new JSONObject(responseText);
            } catch (JSONException e) {
                // Handle error
            }
            if (json != null && json.length() > 0) {
                AsyncConvertImageToBitmap updateImageAsync = new AsyncConvertImageToBitmap();
                try {
                    Bitmap bitmap = updateImageAsync.execute(responseText).get();
                    profileImage.setImageBitmap(bitmap);
                } catch (ExecutionException | InterruptedException e) {
                    // Handle error
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_genericprofileimageicon);
            }
        }
    }

    private String getConstitId(String constit) {
        AsyncSkyApiGet asyncgetCall = new AsyncSkyApiGet();
        try {
            String responseText = asyncgetCall.execute(skyApiUrl + "search?search_text=" + constit, bearerToken, subscriptionKey).get();
            if (responseText != null) {
                try {
                    JSONObject json = new JSONObject(responseText);
                    JSONArray responseList = json.getJSONArray("value");
                    String firstConstit = responseList.get(0).toString();
                    JSONObject firstConstitJson = new JSONObject(firstConstit);
                    return firstConstitJson.getString("id");

                } catch (JSONException e) {
                    // Handle error
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            // Handle error
        }
        return null;
    }

    @SuppressLint("ApplySharedPref")
    private void updateRecordText(String constitId) {
        AsyncSkyApiGet asyncgetCall = new AsyncSkyApiGet();
        try {
            String responseText = asyncgetCall.execute(skyApiUrl + constitId, bearerToken, subscriptionKey).get();
            if (responseText != null) {
                try {
                    JSONObject json = new JSONObject(responseText);
                    nameText.setText(json.getString("name"));

                    // Clickable address link that opens Google Maps
                    JSONObject address = json.getJSONObject("address");
                    String formattedAddress = address.getString("formatted_address");
                    if (formattedAddress.equals("")) {
                        addressText.setText(R.string.no_address);
                    } else {
                        addressText.setText(formattedAddress);
                    }

                    JSONObject phone = json.getJSONObject("phone");
                    String phoneNumber = phone.getString("number");
                    if (phoneNumber.equals("")) {
                        phoneText.setText(R.string.no_phone);
                    } else {
                        phoneText.setText(phoneNumber);
                    }

                } catch (JSONException e) {
                    phoneText.setText(R.string.no_phone);
                    // Handle error
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_skywarningicon);
                nameText.setText(R.string.record_not_found);
                addressText.setText("");
                phoneText.setText("");
                addressPinIcon.setText("");
                phoneIcon.setText("");

            }

            SharedPreferences.Editor editor = constitRecordCache.edit();
            editor.putString("constitName", nameText.getText().toString());
            editor.putString("constitAddress", addressText.getText().toString());
            editor.putString("constitPhone", phoneText.getText().toString());
            editor.commit();
        } catch (ExecutionException | InterruptedException e) {
            // Handle error
        }
    }
}
