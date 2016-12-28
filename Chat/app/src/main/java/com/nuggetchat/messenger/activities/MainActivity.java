package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    LoginButton loginButton;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            if (accessToken.isExpired()) {
                AccessToken.refreshCurrentAccessTokenAsync(
                        new AccessToken.AccessTokenRefreshCallback() {
                            @Override
                            public void OnTokenRefreshed(AccessToken accessToken) {
                                AccessToken.setCurrentAccessToken(accessToken);
                                gotoNextActivity();
                            }

                            @Override
                            public void OnTokenRefreshFailed(FacebookException exception) {
                                Log.e(LOG_TAG, "Error in refreshing access token.");
                            }
                        });
            } else {
                gotoNextActivity();
                return;
            }
        }

        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile", "email", "user_friends");

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loginToFirebase(loginResult);
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

    private void loginToFirebase(final LoginResult loginResult) {
        // App code
        System.out.println("onSuccess");
        final String accessToken = loginResult.getAccessToken().getToken();
        Log.i("accessToken", accessToken);
        FirebaseAuth.getInstance()
                .signInWithCredential(FacebookAuthProvider.getCredential(accessToken))
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        getFirebaseIdToken(task, loginResult.getAccessToken());
                    }
                });

    }

    private void getFirebaseIdToken(final Task<AuthResult> task, final AccessToken accessToken) {
        if (!task.isSuccessful()) {
            Log.e(LOG_TAG, "Error in login.", task.getException());
            return;
        }

        task.getResult().getUser().getToken(true /* forceRefresh */)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> tokenTask) {
                        String firebaseIdToken = tokenTask.getResult().getToken();
                        Log.i(LOG_TAG, "firebaseIdToken " + firebaseIdToken);
                        //getFriendsGraph(accessToken);
                        getUserFriends(accessToken.getToken(), firebaseIdToken, task.getResult().getUser().getUid());
                    }
                });

    }

    private void getFriendsGraph(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.i("LoginActivity", response.toString());
                        // Get facebook data from login
                        Bundle bFacebookData = getFacebookData(object);

                        //Intent intent =  new Intent(MainActivity.this, FriendsManagerActivity
                        // .class);
                        Intent intent = new Intent(MainActivity.this, FriendsManagerActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields",
                "id, first_name, last_name, email,gender, birthday, location"); // Parámetros que
        // pedimos a facebook
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void gotoNextActivity() {
        Intent intent = new Intent(MainActivity.this, GamesChatActivity.class);
        startActivity(intent);
        finish();
    }

    private Bundle getFacebookData(JSONObject object) {

        try {
            Bundle bundle = new Bundle();
            String id = object.getString("id");
            Log.e(LOG_TAG, "getFacebookData: USER ID " + id);
            SharedPreferenceUtility.setFacebookUserId(id, MainActivity.this);
            String name = object.getString("first_name");
            SharedPreferenceUtility.setFacebookUserName(name, MainActivity.this);
            URL profile_pic;

            try {
                profile_pic = new URL("https://graph.facebook.com/" + id
                        + "/picture?width=200&height=150");
                Log.i("profile_pic", profile_pic + "");
                bundle.putString("profile_pic", profile_pic.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }

            bundle.putString("idFacebook", id);
            if (object.has("first_name")) {
                bundle.putString("first_name", object.getString("first_name"));
            }
            if (object.has("last_name")) {
                bundle.putString("last_name", object.getString("last_name"));
            }
            if (object.has("email")) {
                bundle.putString("email", object.getString("email"));
            }
            if (object.has("gender")) {
                bundle.putString("gender", object.getString("gender"));
            }
            if (object.has("birthday")) {
                bundle.putString("birthday", object.getString("birthday"));
            }
            if (object.has("location")) {
                bundle.putString("location", object.getJSONObject("location").getString("name"));
            }

            Log.d(LOG_TAG,
                    "First_name:" + object.getString("first_name") + object.getString("last_name")
                            + object.getString("email") + object.getString("gender") + profile_pic);

            return bundle;
        } catch (JSONException e) {
            Log.d(LOG_TAG, "Error parsing JSON", e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void getUserFriends(final String accessToken, final String idToken, final String firebaseUid) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://server.nuggetchat.com:8080/getFriends";
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG_TAG, "Request success " + response.toString());
                getFriendsFromFirebase(firebaseUid);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Error in making friends request", error);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, accessToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, idToken);
                return params;
            }
        };
        queue.add(sr);
    }

    private void getFriendsFromFirebase(String firebaseId) {
        String firebaseUri = "https://nuggetplay-ceaaf.firebaseio.com/users/" + firebaseId + "/friends";
        Log.i(LOG_TAG, "Fetching user friends : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                for (DataSnapshot dsh : dataSnapshot.getChildren()) {
                    Log.i(LOG_TAG, "datasnapshot, " + dsh.getValue());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
