package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

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
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.FirebaseTokenUtils;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final double VIDEO_ASPECT_RATIO = 0.72;
    LoginButton loginButton;
    CallbackManager callbackManager;
    Handler mainHandler;
    VideoView nuggetLoginAnim;
    private NuggetInjector nuggetInjector;
    private Uri videoPath;
    @BindView(R.id.video_placeholder)
    FrameLayout videoPlaceholder;

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
                                Log.e(LOG_TAG, "Error in refreshing access token.");
                            }
                        });
            } else {
                gotoGameChatActivity();
                return;
            }
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        nuggetInjector = NuggetInjector.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());

        nuggetLoginAnim = (VideoView) findViewById(R.id.nugget_login_anim);
        videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.logo_anim);
//        setVideoPlayerDimension();

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

    @Override
    protected void onResume() {
        super.onResume();

        nuggetLoginAnim.setVideoURI(videoPath);
        nuggetLoginAnim.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e(LOG_TAG, "Error with the splash video player. Error code::" + extra);
                // This signifies that error is not handled, so if false returned then
                // onCompletionListener is called.
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            nuggetLoginAnim.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        // video started; hide the placeholder.
                        videoPlaceholder.setVisibility(View.GONE);
                        return true;
                    }
                    return false;
                }
            });
        } else {
            videoPlaceholder.setVisibility(View.GONE);
        }
        nuggetLoginAnim.start();

    }

    

    private void loginToFirebase(final LoginResult loginResult) {
        loginProgressBar.setVisibility(View.VISIBLE);
        // App code
        final String accessToken = loginResult.getAccessToken().getToken();
        Log.i(LOG_TAG, "Trying Login");
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
            Log.e(LOG_TAG, "Error in login.", task.getException());
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
        Log.d(LOG_TAG, "Storing firebase id at: " + facebookToFirebaseMap);
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl(facebookToFirebaseMap);
        if (firebaseRef == null) {
            return;
        }
        firebaseRef.setValue(firebaseId);
    }

    private void setVideoPlayerDimension() {
        // Adjust the size of the video
        // so it fits on the screen
        int screenWidth = nuggetInjector.getScreenLandscapeWidth();
        int screenHeight = nuggetInjector.getScreenLandscapeHeight();
        double screenAspectRatio = (double) screenHeight / (double) screenWidth;
        ViewGroup.LayoutParams layoutParams = nuggetLoginAnim.getLayoutParams();

        if (VIDEO_ASPECT_RATIO < screenAspectRatio) {
            layoutParams.width = screenWidth;
            layoutParams.height = (int) (screenWidth * VIDEO_ASPECT_RATIO);

        } else {
            layoutParams.height = screenHeight;
            layoutParams.width = (int) (screenHeight / VIDEO_ASPECT_RATIO);
        }
        nuggetLoginAnim.setLayoutParams(layoutParams);
    }

    private void saveFacebookIdAndStartNextActivity() {
        final String firebaseId = SharedPreferenceUtility.getFirebaseUid(this);
        String firebaseUri = Conf.firebaseUsersUri() + firebaseId + "/facebookId";
        Log.i(LOG_TAG, "Fetching user's facebook id: " + firebaseUri);

        final DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String facebookUserId = dataSnapshot.getValue().toString();
                    SharedPreferenceUtility.setFacebookUserId(facebookUserId, MainActivity.this);
                    Log.i(LOG_TAG, "Facebook id " + facebookUserId);
                    FirebaseTokenUtils.saveAllDeviceRegistrationToken(firebaseId, facebookUserId, MainActivity.this);
                    saveFacebookToFirebaseMap(facebookUserId, firebaseId);
                    startFriendManagerActivity();
                } else {
                    Log.i(LOG_TAG, "No firebase id yet in server");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "Facebook id fetch cancelled");
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
        Log.i(LOG_TAG, "Refresh friends Firebase token " + firebaseToken);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, Conf.GET_FRIENDS_API_URL,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(LOG_TAG, "Facebook login success ");
                saveFacebookIdAndStartNextActivity();
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
                params.put(RequestParams.FACEBOOK_ACCESS_TOKEN, facebookToken);
                params.put(RequestParams.FIREBASE_ID_TOKEN, firebaseToken);
                return params;
            }
        };
        queue.add(sr);
    }
}
