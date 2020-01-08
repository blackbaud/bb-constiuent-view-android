package com.blackbaud.constitview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private SharedPreferences sharedPreferences;
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

        sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);
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

    // Parse the response from the auth SPA and turn it into a json style string
    public static String paramJson(String paramIn) {
        paramIn = paramIn.replaceAll("=", "\":\"");
        paramIn = paramIn.replaceAll("&", "\",\"");
        return "{\"" + paramIn + "\"}";
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
                        setCachedData(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Get, clean up, and store token info
    @SuppressLint("ApplySharedPref")
    private void setCachedData(JSONObject json) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // Get date response into a Java parsable date string and store
            String tokenExpiration = json.getString("expires");
            String tokenDate = tokenExpiration.split("T")[0];
            String tokenTime = tokenExpiration.split("T")[1];
            String expires = tokenDate + " " + tokenTime;
            editor.putString("expires", expires);

            // Store current token
            bearerToken = "Bearer " + json.getString("access_token");
            editor.putString("bearerToken", bearerToken);

            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateRecordImage(String constitId) {
        AsyncUpdateConstitImage asyncUpdateConstitImage = new AsyncUpdateConstitImage();
        asyncUpdateConstitImage.execute(skyApiUrl + constitId + "/profilepicture");
    }

    private String getConstitId(String constit) {
        AsyncGetCall asyncgetCall = new AsyncGetCall();
        try {
            String responseText = asyncgetCall.execute(skyApiUrl + "search?search_text=" + constit).get();
            if (responseText != null) {
                try {
                    JSONObject json = new JSONObject(responseText);
                    JSONArray responseList = json.getJSONArray("value");
                    String firstConstit = responseList.get(0).toString();
                    JSONObject firstConstitJson = new JSONObject(firstConstit);
                    return firstConstitJson.getString("id");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("ApplySharedPref")
    private void updateRecordText(String constitId) {
        AsyncGetCall asyncgetCall = new AsyncGetCall();
        try {
            String responseText = asyncgetCall.execute(skyApiUrl + constitId).get();
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
                    e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncGetCall extends AsyncTask<String, String, String> {

        private String responseText = null;

        @Override
        protected String doInBackground(String... strings) {
            String reqUrl = strings[0];

            // Maintain http url connection.
            HttpURLConnection httpConn = null;

            // Read text input stream.
            InputStreamReader isReader = null;

            // Read text into buffer.
            BufferedReader bufReader = null;

            // Save server response text.
            StringBuilder readTextBuf = new StringBuilder();

            try {
                // Create a URL object use page url.
                URL url = new URL(reqUrl);

                // Open http connection to web server.
                httpConn = (HttpURLConnection)url.openConnection();

                // Headers
                httpConn.setRequestProperty("Authorization", bearerToken);
                httpConn.setRequestProperty("Bb-Api-Subscription-Key", subscriptionKey);

                // Set http request method to get.
                httpConn.setRequestMethod("GET");

                // Set connection timeout and read timeout value.
                httpConn.setConnectTimeout(10000);
                httpConn.setReadTimeout(10000);

                // Get input stream from web url connection.
                InputStream inputStream = httpConn.getInputStream();

                // Create input stream reader based on url connection input stream.
                isReader = new InputStreamReader(inputStream);

                // Create buffered reader.
                bufReader = new BufferedReader(isReader);

                // Read line of text from server response.
                String line = bufReader.readLine();

                // Loop while return line is not null.
                while(line != null)
                {
                    // Append the text to string buffer.
                    readTextBuf.append(line);

                    // Continue to read text line.
                    line = bufReader.readLine();
                }

                // Send message to main thread to update response text in TextView after read all.
                Message message = new Message();

                // Set message type.
                message.what = REQUEST_CODE_SHOW_RESPONSE_TEXT;

                // Create a bundle object.
                Bundle bundle = new Bundle();
                // Put response text in the bundle with the special key.
                bundle.putString(KEY_RESPONSE_TEXT, readTextBuf.toString());
                // Set bundle data in message.
                message.setData(bundle);
                // Send message to main thread Handler to process.
                if(message.what == REQUEST_CODE_SHOW_RESPONSE_TEXT)
                {
                    Bundle msgBundle = message.getData();
                    if(msgBundle != null)
                    {
                        responseText = bundle.getString(KEY_RESPONSE_TEXT);
                    }
                }
                httpConn.disconnect();
            } catch(IOException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            } finally {
                try {
                    if (bufReader != null) {
                        bufReader.close();
                    }

                    if (isReader != null) {
                        isReader.close();
                    }

                    if (httpConn != null) {
                        httpConn.disconnect();
                    }
                } catch (IOException ex) {
                    Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
                }
            }

            return responseText;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncUpdateConstitImage extends AsyncTask<String, String, String> {

        private String responseText = null;

        @Override
        protected String doInBackground(String... strings) {
            String reqUrl = strings[0];

            // Maintain http url connection.
            HttpURLConnection httpConn = null;

            // Read text input stream.
            InputStreamReader isReader = null;

            // Read text into buffer.
            BufferedReader bufReader = null;

            // Save server response text.
            StringBuilder readTextBuf = new StringBuilder();

            try {
                // Create a URL object use page url.
                URL url = new URL(reqUrl);

                // Open http connection to web server.
                httpConn = (HttpURLConnection)url.openConnection();

                // Headers
                httpConn.setRequestProperty("Authorization", bearerToken);
                httpConn.setRequestProperty("Bb-Api-Subscription-Key", subscriptionKey);

                // Set http request method to get.
                httpConn.setRequestMethod("GET");

                // Set connection timeout and read timeout value.
                httpConn.setConnectTimeout(10000);
                httpConn.setReadTimeout(10000);

                // Get input stream from web url connection.
                InputStream inputStream = httpConn.getInputStream();

                // Create input stream reader based on url connection input stream.
                isReader = new InputStreamReader(inputStream);

                // Create buffered reader.
                bufReader = new BufferedReader(isReader);

                // Read line of text from server response.
                String line = bufReader.readLine();

                // Loop while return line is not null.
                while(line != null)
                {
                    // Append the text to string buffer.
                    readTextBuf.append(line);

                    // Continue to read text line.
                    line = bufReader.readLine();
                }

                // Send message to main thread to update response text in TextView after read all.
                Message message = new Message();

                // Set message type.
                message.what = REQUEST_CODE_SHOW_RESPONSE_TEXT;

                // Create a bundle object.
                Bundle bundle = new Bundle();
                // Put response text in the bundle with the special key.
                bundle.putString(KEY_RESPONSE_TEXT, readTextBuf.toString());
                // Set bundle data in message.
                message.setData(bundle);
                // Send message to main thread Handler to process.
                if(message.what == REQUEST_CODE_SHOW_RESPONSE_TEXT)
                {
                    Bundle msgBundle = message.getData();
                    if(msgBundle != null)
                    {
                        responseText = bundle.getString(KEY_RESPONSE_TEXT);

                    }
                }
                httpConn.disconnect();
            } catch(IOException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            } finally {
                try {
                    if (bufReader != null) {
                        bufReader.close();
                    }

                    if (isReader != null) {
                        isReader.close();
                    }

                    if (httpConn != null) {
                        httpConn.disconnect();
                    }
                } catch (IOException ex) {
                    Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
                }
            }

            return responseText;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPreExecute();
            ImageView profileImage = findViewById(R.id.profileImage);
            if (responseText != null) {
                JSONObject json = null;
                try {
                    json = new JSONObject(responseText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (json != null && json.length() > 0) {
                    UpdateImageAsync updateImageAsync = new UpdateImageAsync();
                    try {
                        Bitmap bitmap = updateImageAsync.execute(responseText).get();
                        profileImage.setImageBitmap(bitmap);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_genericprofileimageicon);
                }
            }
        }

        @SuppressLint("StaticFieldLeak")
        private class UpdateImageAsync extends AsyncTask<String, String, Bitmap> {

            @Override
            protected Bitmap doInBackground(String... strings) {
                try {
                    // Get image URL
                    JSONObject json = new JSONObject(responseText);
                    return BitmapFactory.decodeStream((InputStream)new URL(json.getString("url")).getContent());
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }
}
