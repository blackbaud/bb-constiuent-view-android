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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;

public class ConstitRecord extends AppCompatActivity {

    private String skyApiUrl = "https://api.sky.blackbaud.com/constituent/v1/constituents/280";

    //TODO: Remove subscriptionKey later
    private String subscriptionKey = "f6b4ed3cc9ef41d19195cb3f7ac49b45";

    //TODO: Save response info to local storage and handle expired token
    private String bearerToken = null;

    // Debug log tag.
    private static final String TAG_HTTP_URL_CONNECTION = "HTTP_URL_CONNECTION";

    // Child thread sent message type value to activity main thread Handler.
    private static final int REQUEST_CODE_SHOW_RESPONSE_TEXT = 1;

    // The key of message stored server returned data.
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";

    private TextView nameText = null;
    private TextView addressText = null;
    private TextView phoneText = null;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constit_record);

        // eraseCachedData();

        nameText = findViewById(R.id.constitNameText);
        addressText = findViewById(R.id.constitAddressText);
        phoneText = findViewById(R.id.constitPhoneText);

        sharedPreferences = getSharedPreferences("TokenCache", Context.MODE_PRIVATE);

        bearerToken = sharedPreferences.getString("bearerToken", null);

        handleIntent(getIntent());
        updateRecordImage();
        updateRecordText();
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

    // TODO: delete this later
    @SuppressLint("ApplySharedPref")
    private void eraseCachedData() {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
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

    private void updateRecordImage() {
        AsyncUpdateConstitImage asyncUpdateConstitImage = new AsyncUpdateConstitImage();
        asyncUpdateConstitImage.execute();
    }

    private void updateRecordText() {
        AsyncUpdateConstitText asyncUpdateConstitText = new AsyncUpdateConstitText();
        try {
            String responseText = asyncUpdateConstitText.execute().get();
            if (responseText != null) {

                try {
                    JSONObject json = new JSONObject(responseText);
                    nameText.setText(json.getString("name"));

                    // Clickable address link that opens Google Maps
                    JSONObject address = json.getJSONObject("address");
                    addressText.setText(address.getString("formatted_address"));

                    JSONObject phone = json.getJSONObject("phone");
                    phoneText.setText(phone.getString("number"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class AsyncUpdateConstitText extends AsyncTask<String, String, String> {

        public String responseText = null;

        @Override
        protected String doInBackground(String... strings) {

            String reqUrl = skyApiUrl;

            // Maintain http url connection.
            HttpURLConnection httpConn = null;

            // Read text input stream.
            InputStreamReader isReader = null;

            // Read text into buffer.
            BufferedReader bufReader = null;

            // Save server response text.
            StringBuffer readTextBuf = new StringBuffer();

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
            }catch(MalformedURLException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            }catch(IOException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            }finally {
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

    private class AsyncUpdateConstitImage extends AsyncTask<String, String, String> {

        public String responseText = null;

        @Override
        protected String doInBackground(String... strings) {
            String reqUrl = skyApiUrl + "/profilepicture";

            // Maintain http url connection.
            HttpURLConnection httpConn = null;

            // Read text input stream.
            InputStreamReader isReader = null;

            // Read text into buffer.
            BufferedReader bufReader = null;

            // Save server response text.
            StringBuffer readTextBuf = new StringBuffer();

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
            }catch(MalformedURLException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            }catch(IOException ex)
            {
                Log.e(TAG_HTTP_URL_CONNECTION, ex.getMessage(), ex);
            }finally {
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
            if (responseText != null) {
                UpdateImageAsync updateImageAsync = new UpdateImageAsync();
                try {
                    Bitmap bitmap = updateImageAsync.execute(responseText).get();
                    ImageView profileImage = findViewById(R.id.profileImage);
                    profileImage.setImageBitmap(bitmap);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private class UpdateImageAsync extends AsyncTask<String, String, Bitmap> {

            @Override
            protected Bitmap doInBackground(String... strings) {
                try {
                    // Get image URL
                    JSONObject json = new JSONObject(responseText);
                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(json.getString("url")).getContent());
                    return bitmap;
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }
}
