package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

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
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    LoginButton loginButton;
    CallbackManager callbackManager;

    @BindView(R.id.login_progress_bar) /* package-local */ ProgressBar loginProgressBar;

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
        ButterKnife.bind(this);
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
        loginProgressBar.setVisibility(View.VISIBLE);
        // App code
        final String accessToken = loginResult.getAccessToken().getToken();
        Log.i(LOG_TAG, "Trying Login");
//      Log.i(LOG_TAG, "firebase token " + accessToken);
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
//                        Log.i(LOG_TAG, "firebaseIdToken " + firebaseIdToken);
                        //getFriendsGraph(accessToken);
                        SharedPreferenceUtility.setFacebookAccessToken(accessToken.getToken(), MainActivity.this);
                        SharedPreferenceUtility.setFirebaseIdToken(firebaseIdToken, MainActivity.this);
                        SharedPreferenceUtility.setFirebaseUid(task.getResult().getUser().getUid(), MainActivity.this);
                        getUserFriends(SharedPreferenceUtility.getFacebookAccessToken(MainActivity.this), SharedPreferenceUtility.getFirebaseIdToken(MainActivity.this), SharedPreferenceUtility.getFirebaseUid(MainActivity.this));
                    }
                });

    }

    private void setUserFacebookUserId() {
        String firebaseUri = "https://nuggetplay-ceaaf.firebaseio.com/users/" + SharedPreferenceUtility.getFirebaseUid(this) + "/facebookId";
        Log.i(LOG_TAG, "Fetching user friends : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SharedPreferenceUtility.setFacebookUserId(dataSnapshot.getValue().toString(), MainActivity.this);
                Log.d(LOG_TAG, SharedPreferenceUtility.getFacebookUserId(MainActivity.this));
                Intent intent = new Intent(MainActivity.this, FriendsManagerActivity.class);
                startActivity(intent);
                loginProgressBar.setVisibility(View.INVISIBLE);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void gotoNextActivity() {
        Intent intent = new Intent(MainActivity.this, GamesChatActivity.class);
        startActivity(intent);
        finish();
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
                Log.i(LOG_TAG, "Facebook login success ");
//                Log.i(LOG_TAG, "Facebook response " + response);
                setUserFacebookUserId();
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
}
