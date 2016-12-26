package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    LoginButton loginButton;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AccessToken.getCurrentAccessToken() != null) {
            Intent intent =  new Intent(MainActivity.this, FriendsManagerActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile", "email", "user_friends");

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                System.out.println("onSuccess");
                String accessToken = loginResult.getAccessToken().getToken();
                Log.i("accessToken", accessToken);

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.i("LoginActivity", response.toString());
                        // Get facebook data from login
                        Bundle bFacebookData = getFacebookData(object);

                        Intent intent =  new Intent(MainActivity.this, FriendsManagerActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, email,gender, birthday, location"); // Parámetros que pedimos a facebook
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                // App code
                Log.i(LOG_TAG, "ON CANCEL");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code'
                Log.i(LOG_TAG, "ON ERROR", exception);
            }
        });
    }

    private Bundle getFacebookData(JSONObject object) {

        try {
            Bundle bundle = new Bundle();
            String id = object.getString("id");
            SharedPreferenceUtility.setFacebookUserId(id, MainActivity.this);
            String name = object.getString("first_name");
            SharedPreferenceUtility.setFacebookUserName(name, MainActivity.this);
            URL profile_pic;

            try {
                profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=200&height=150");
                Log.i("profile_pic", profile_pic + "");
                bundle.putString("profile_pic", profile_pic.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }

            bundle.putString("idFacebook", id);
            if (object.has("first_name"))
                bundle.putString("first_name", object.getString("first_name"));
            if (object.has("last_name"))
                bundle.putString("last_name", object.getString("last_name"));
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));
            if (object.has("gender"))
                bundle.putString("gender", object.getString("gender"));
            if (object.has("birthday"))
                bundle.putString("birthday", object.getString("birthday"));
            if (object.has("location"))
                bundle.putString("location", object.getJSONObject("location").getString("name"));

            Log.d(LOG_TAG, "First_name:" + object.getString("first_name") + object.getString("last_name") + object.getString("email") + object.getString("gender") + profile_pic);

            return bundle;
        }
        catch(JSONException e) {
            Log.d(LOG_TAG,"Error parsing JSON", e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {






























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
