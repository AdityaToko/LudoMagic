package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.graphics.Typeface;
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
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.RequestParams;
import com.nuggetchat.messenger.base.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.AnalyticConstants;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

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
    protected AlphaAnimation fadeInNuggetLine = new AlphaAnimation(0.0f , 1.0f ) ;
    protected AlphaAnimation fadeInText1 = new AlphaAnimation(0.0f , 1.0f ) ;
    protected AlphaAnimation fadeInText2 = new AlphaAnimation(0.0f , 1.0f ) ;
    protected AlphaAnimation fadeInText3 = new AlphaAnimation(0.0f , 1.0f ) ;
    protected AlphaAnimation fadeInText4 = new AlphaAnimation(0.0f , 1.0f ) ;
    protected AlphaAnimation fadeInFBButton = new AlphaAnimation(0.0f , 1.0f ) ;

    private boolean restartAnimOnResume = true;
    private Button fbOverlayButton;

    @BindView(R.id.video_placeholder)  FrameLayout videoPlaceholder;
    @BindView(R.id.login_anim_overlay)  ImageView loginAnimOverlay;
    @BindView(R.id.nugget_line) TextView nuggetLine;
    @BindView(R.id.login_page_text1) TextView loginPageText1;
    @BindView(R.id.login_page_text2) TextView loginPageText2;
    @BindView(R.id.login_page_text3) TextView loginPageText3;
    @BindView(R.id.login_page_text4) TextView loginPageText4;

    @BindView(R.id.login_progress_bar) /* package-local */ ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nuggetInjector = NuggetInjector.getInstance();

        ViewUtils.startAppseeAnalytics(getString(R.string.appsee_id), LOG_TAG);

        // Init AppFlyer for install tracking, pass mixpanel id
        nuggetInjector.getAppsFlyer().initTracking();

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

        nuggetInjector.getMixpanel().logCreateView(LOG_TAG);

        videoPlaceholder.setVisibility(View.VISIBLE);
        mainHandler = new Handler(Looper.getMainLooper());

        Typeface font = Typeface.createFromAsset(getAssets(), "handwriting.ttf");
        nuggetLine.setTypeface(font);

        setAnimationValues();

        nuggetLoginAnim = (VideoView) findViewById(R.id.nugget_login_anim);
        videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.logo_anim);

        fbOverlayButton = (Button) findViewById(R.id.fb_overlay_button);
        fbOverlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nuggetInjector.getMixpanel()
                        .startTimer(AnalyticConstants.FACEBOOK_LOGIN_TIMER);
                restartAnimOnResume = false;
                loginAnimOverlay.setVisibility(View.VISIBLE);
                loginButton.performClick();
            }
        });

        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile", "email", "user_friends");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                nuggetInjector.getMixpanel()
                        .stopTimer(AnalyticConstants.FACEBOOK_LOGIN_TIMER);
                loginToFirebase(loginResult);
            }

            @Override
            public void onCancel() {
                nuggetInjector.getMixpanel()
                        .track(AnalyticConstants.FACEBOOK_LOGIN_RESULT, "Cancelled");
                MyLog.i(LOG_TAG, "ON CANCEL");
            }

            @Override
            public void onError(FacebookException exception) {
                nuggetInjector.getMixpanel()
                        .track(AnalyticConstants.FACEBOOK_LOGIN_RESULT, "Error");
                MyLog.i(LOG_TAG, "ON ERROR", exception);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(restartAnimOnResume) {
            textAnimations();
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
                            videoPlaceholder.setVisibility(View.INVISIBLE);
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

    }

    private void setAnimationValues() {
        fadeInNuggetLine.setDuration(600);
        fadeInText1.setDuration(1200);
        fadeInText2.setDuration(1200);
        fadeInText3.setDuration(1200);
        fadeInText4.setDuration(1200);
        fadeInFBButton.setDuration(1200);

        fadeInNuggetLine.setFillAfter(true);
        fadeInText1.setFillAfter(true);
        fadeInText2.setFillAfter(true);
        fadeInText3.setFillAfter(true);
        fadeInText4.setFillAfter(true);
        fadeInFBButton.setFillAfter(true);

        fadeInNuggetLine.setStartOffset(2200);
        fadeInText1.setStartOffset(2600);
        fadeInText2.setStartOffset(3200);
        fadeInText3.setStartOffset(3800);
        fadeInText4.setStartOffset(4400);
        fadeInFBButton.setStartOffset(5000);
    }

    private void textAnimations() {
        nuggetLine.startAnimation(fadeInNuggetLine);
        loginPageText1.startAnimation(fadeInText1);
        loginPageText2.startAnimation(fadeInText2);
        loginPageText3.startAnimation(fadeInText3);
        loginPageText4.startAnimation(fadeInText4);
//      loginButton.startAnimation(fadeInFBButton);
    }

    private void loginToFirebase(final LoginResult facebookLoginResult) {
        loginProgressBar.setVisibility(View.VISIBLE);
        // App code
        final AccessToken facebookAccessToken = facebookLoginResult.getAccessToken();
        nuggetInjector.getMixpanel().startTimer(AnalyticConstants.FIREBASE_LOGIN_TIMER);
        MyLog.i(LOG_TAG, "Firebase Login");
        FirebaseAuth.getInstance()
                .signInWithCredential(FacebookAuthProvider.getCredential(facebookAccessToken.getToken()))
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        getFirebaseIdTokenAndStartNextActivity(task, facebookAccessToken);
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
                        nuggetInjector.getMixpanel()
                                .stopTimer(AnalyticConstants.FIREBASE_LOGIN_TIMER);
                        String facebookToken = accessToken.getToken();
                        String firebaseIdToken = tokenTask.getResult().getToken();
                        String firebaseUid = task.getResult().getUser().getUid();
                        String facebookName = task.getResult().getUser().getDisplayName();

                        SharedPreferenceUtility.setFacebookAccessToken(facebookToken, MainActivity.this);
                        SharedPreferenceUtility.setFirebaseUid(firebaseUid, MainActivity.this);
                        SharedPreferenceUtility.setFacebookUserName(facebookName, MainActivity.this);
                        nuggetInjector.getMixpanel().loginUserAndUpdateUserDetails(firebaseUid);
                        nuggetInjector.getAppsFlyer().setUserId(firebaseUid);
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
        String facebookUserId = Profile.getCurrentProfile().getId();
        SharedPreferenceUtility.setFacebookUserId(facebookUserId, MainActivity.this);
        MyLog.i(LOG_TAG, "Facebook id " + facebookUserId);
        saveFacebookToFirebaseMap(facebookUserId, firebaseId);
        startFriendManagerActivity();
    }

    private void startFriendManagerActivity() {
        nuggetInjector.getMixpanel().track(AnalyticConstants.LOGGED_IN_USER,
                Profile.getCurrentProfile().getName());
        Intent intent = new Intent(MainActivity.this, FriendsManagerActivity.class);
        startActivity(intent);
        loginProgressBar.setVisibility(View.INVISIBLE);
        finish();
    }

    private void gotoGameChatActivity() {
        nuggetInjector.getMixpanel().track(AnalyticConstants.RETURNING_USER, "I am back");
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
        MyLog.i(LOG_TAG, "Refresh friends Firebase token");
        nuggetInjector.getMixpanel().startTimer(AnalyticConstants.GET_FRIENDS_TIMER);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, Conf.GET_FRIENDS_API_URL,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                MyLog.i(LOG_TAG, "Facebook login success ");
                nuggetInjector.getMixpanel().stopTimer(AnalyticConstants.GET_FRIENDS_TIMER);
                saveFacebookIdAndStartNextActivity();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                nuggetInjector.getMixpanel().trackError(LOG_TAG, "Friend request", error);
                MyLog.e(LOG_TAG, "Error in making friends request", error);
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
