package com.nuggetchat.messenger.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.activities.AudioPlayer;
import com.nuggetchat.messenger.activities.ChatFragment;
import com.nuggetchat.messenger.activities.GamesChatActivity;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IncomingCallActivity extends AppCompatActivity {
    private static final String LOG_TAG = IncomingCallActivity.class.getSimpleName();
    public static final String CALL_ACCEPTED = "call_accepted";
    @BindView(R.id.accept_btn)
    public Button acceptButton;
    @BindView(R.id.reject_btn)
    public Button rejectButton;
    @BindView(R.id.caller_name_txt)
    public TextView callerName;
    @BindView(R.id.caller_image)
    public ImageView callerImage;
    private AudioPlayer audioPlayer;
    private boolean isActivityForResult;
    private BroadcastReceiver broadcastReceiver;
    private Handler mainHandler;
    private Handler handler;

    Bundle bundle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewUtils.startAppseeAnalytics(getString(R.string.appsee_id), LOG_TAG);

        if (!NuggetInjector.getInstance().isChatServiceRunning()){
            Log.d(LOG_TAG, "onCreate: Chat Service started from Incoming Call");
            startService(new Intent(this, ChatService.class));
        } else {
            Log.d(LOG_TAG, "onCreate: Chat Service already running");
        }

        MyLog.i(LOG_TAG, "onCreate IncomingCallActivity");
        mainHandler = new Handler(Looper.getMainLooper());
        handler = new Handler();
        Intent intent = getIntent();
        bundle = intent.getExtras();
        (NuggetInjector.getInstance()).setIncomingCall(true);

        String type = bundle.getString("type");
        String from = bundle.getString("from");
        String to = bundle.getString("to");
        String token = bundle.getString("token");
        isActivityForResult = false;
        if ("chat_frag".equals(bundle.getString("from_activity"))) {
            isActivityForResult = true;
        }

        MyLog.e(LOG_TAG, "Type: " + type + " From: " + from + " To: " + to + " Token: " + token);

        fetchFriendNameAndPic(from);

        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        audioPlayer = AudioPlayer.getInstance(this);
        audioPlayer.requestAudioFocus();
        audioPlayer.playRingtone(AudioPlayer.CALLER_TUNE);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.nuggetchat.messenger.DISMISS_INCOMING_CALL_ACTIVITY")) {
                    unregisterReceiver(this);
                    audioPlayer.stopRingtone();
                    (NuggetInjector.getInstance()).setIncomingCall(false);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                rejectButtonClick();
            }
        }, 30000);
        MyLog.i(LOG_TAG, "onResume IncomingCallActivit");
        IntentFilter intentFilter = new IntentFilter("com.nuggetchat.messenger.DISMISS_INCOMING_CALL_ACTIVITY");
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void fetchFriendNameAndPic(final String callerFacebookId) {

        String userFirebaseId = SharedPreferenceUtility.getFirebaseUid(IncomingCallActivity.this);
        String usersFriendUri = Conf.firebaseUserFriend(userFirebaseId, callerFacebookId);
        if (Utils.isNullOrEmpty(usersFriendUri)) {
            MyLog.w(LOG_TAG, "No Caller facebook url");
            return;
        }
        FirebaseDatabase.getInstance().getReferenceFromUrl(usersFriendUri)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    String friendName = getString(R.string.new_friend);
                    FriendInfo friendInfo = dataSnapshot.getValue(FriendInfo.class);
                    if (friendInfo != null) {
                        friendName = friendInfo.getName();
                        if (Utils.isNullOrEmpty(friendName)) {
                            MyLog.w(LOG_TAG, "Friend name not set " + friendName);
                        }
                    } else {
                        MyLog.w(LOG_TAG, "Caller not in user friends");
                    }
                    updateUIWithPicAndName(friendName, UserInfo.getUserPic(callerFacebookId));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateUIWithPicAndName(final String userName, final String callerPic) {
        MyLog.i(LOG_TAG, "Updating name:" + userName + " pic:" + callerPic);
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callerName.setText(userName);
                Glide.with(getApplicationContext()).load(callerPic).asBitmap()
                        .centerCrop().into(new BitmapImageViewTarget(callerImage) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory
                                        .create(getApplicationContext().getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        callerImage.setImageDrawable(circularBitmapDrawable);
                    }
                });
            }
        });
    }

    @OnClick(R.id.accept_btn)
    public void acceptButtonClick(){
        (NuggetInjector.getInstance()).setIncomingCall(false);
        (NuggetInjector.getInstance()).setOngoingCall(true);
        handler.removeCallbacksAndMessages(null);
        triggerUserAction(true /*accepted*/);
    }

    @OnClick(R.id.reject_btn)
    public void rejectButtonClick(){
        (NuggetInjector.getInstance()).setIncomingCall(false);
        handler.removeCallbacksAndMessages(null);
        triggerUserAction(false /*accepted*/);
    }

    private void triggerUserAction(boolean accepted) {
        MyLog.i(LOG_TAG, "accepted or rejected, " + accepted);
        audioPlayer.stopRingtone();
        if(!accepted){
            audioPlayer.releaseAudioFocus();
        }
        if (!isActivityForResult) {
            MyLog.i(LOG_TAG, "MessageHandler Trigger - Start game chat activity ");
            Intent startChatIntent = new Intent(this, GamesChatActivity.class);
            bundle.putBoolean(CALL_ACCEPTED, accepted);
            startChatIntent.putExtras(bundle);
            startActivity(startChatIntent);
        } else {
            MyLog.i(LOG_TAG, "MessageHandler Trigger - Restart game chat activity ");
            Intent startChatIntent = new Intent();
            bundle.putBoolean(CALL_ACCEPTED, accepted);
            startChatIntent.putExtras(bundle);
            setResult(ChatFragment.INCOMING_CALL_CODE, startChatIntent);
        }
        finish();
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            MyLog.e(LOG_TAG, "Exception while unregistering");
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
