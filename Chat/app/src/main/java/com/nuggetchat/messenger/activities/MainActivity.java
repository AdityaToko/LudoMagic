package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.FirebaseTokenUtils;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    LoginButton loginButton;
    CallbackManager callbackManager;
    Handler mainHandler;

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
                                gotoGameChatActivity();
                            }

                            @Override
                            public void OnTokenRefreshFailed(FacebookException exception) {
                                MyLog.e(LOG_TAG, "Error in refreshing access token.");
                            }
                        });
            } else {
                gotoGameChatActivity();
                return;
            }
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mainHandler = new Handler(Looper.getMainLooper());
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
                MyLog.i(LOG_TAG, "ON CANCEL");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code'
                MyLog.i(LOG_TAG, "ON ERROR", exception);
            }
        });
    }

    private void loginToFirebase(final LoginResult loginResult) {
        loginProgressBar.setVisibility(View.VISIBLE);
        // App code
        final String accessToken = loginResult.getAccessToken().getToken();
        MyLog.i(LOG_TAG, "Trying Login");
        FirebaseAuth.getInstance()
                .signInWithCredential(FacebookAuthProvider.getCredential(accessToken))
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        getFirebaseIdTokenAndStartNextActivity(task, loginResult.getAccessToken());
                    }
                });
    }

    private void getFirebaseIdTokenAndStartNextActivity(final Task<AuthResult> task,
                                                        final AccessToken accessToken) {
        if (!task.isSuccessful()) {
            MyLog.e(LOG_TAG, "Error in login.", task.getException());
            return;
        }

        task.getResult().getUser().getToken(true /* forceRefresh */)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> tokenTask) {
                        String facebookToken = accessToken.getToken();
                        String firebaseIdToken = tokenTask.getResult().getToken();
                        String firebaseUid = task.getResult().getUser().getUid();
                        String facebookName = task.getResult().getUser().getDisplayName();

                        SharedPreferenceUtility.setFacebookAccessToken(facebookToken, MainActivity.this);
                        SharedPreferenceUtility.setFirebaseUid(firebaseUid, MainActivity.this);
                        SharedPreferenceUtility.setFacebookUserName(facebookName, MainActivity.this);

                        refreshUserFriendsAndStartNextActivity(facebookToken, firebaseIdToken);
                    }
                });

    }

    private void saveFacebookToFirebaseMap(String facebookUserId, String firebaseId) {
        String facebookToFirebaseMap = Conf.firebaseFbToFireidUri(facebookUserId);
        MyLog.d(LOG_TAG, "Storing firebase id at: " + facebookToFirebaseMap);
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl(facebookToFirebaseMap);
        if (firebaseRef == null) {
            return;
        }
        firebaseRef.setValue(firebaseId);
    }

    private void saveFacebookIdAndStartNextActivity() {
        final String firebaseId = SharedPreferenceUtility.getFirebaseUid(this);
        String firebaseUri = Conf.firebaseUsersUri() + firebaseId + "/facebookId";
        MyLog.i(LOG_TAG, "Fetching user's facebook id: " + firebaseUri);

        final DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String facebookUserId = dataSnapshot.getValue().toString();
                    SharedPreferenceUtility.setFacebookUserId(facebookUserId, MainActivity.this);
                    MyLog.i(LOG_TAG, "Facebook id " + facebookUserId);
                    FirebaseTokenUtils.saveAllDeviceRegistrationToken(firebaseId, facebookUserId, MainActivity.this);
                    saveFacebookToFirebaseMap(facebookUserId, firebaseId);
                    startFriendManagerActivity();
                } else {
                    MyLog.i(LOG_TAG, "No firebase id yet in server");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MyLog.w(LOG_TAG, "Facebook id fetch cancelled");
            }
        });
    }

    private void startFriendManagerActivity() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, FriendsManagerActivity.class);
                startActivity(intent);
                loginProgressBar.setVisibility(View.INVISIBLE);
                finish();
            }
        });
    }

    private void gotoGameChatActivity() {
        Intent intent = GamesChatActivity.getNewIntentGameChatActivity(MainActivity.this);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void refreshUserFriendsAndStartNextActivity(final String facebookToken, final String firebaseToken) {
        MyLog.i(LOG_TAG, "Refresh friends Firebase token " + firebaseToken);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, Conf.GET_FRIENDS_API_URL,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                MyLog.i(LOG_TAG, "Facebook login success ");
                saveFacebookIdAndStartNextActivity();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                MyLog.d(LOG_TAG, "Error in making friends request", error);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, facebookToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, firebaseToken);
                return params;
            }
        };
        queue.add(sr);
    }
}
