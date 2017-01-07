package com.nuggetchat.messenger.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.NuggetApplication;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.activities.AudioPlayer;
import com.nuggetchat.messenger.activities.ChatFragment;
import com.nuggetchat.messenger.activities.GamesChatActivity;

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

    Bundle bundle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        bundle = intent.getExtras();
        ((NuggetApplication)getApplication()).setIncomingCall(true);

        String type = bundle.getString("type");
        String from = bundle.getString("from");
        String to = bundle.getString("to");
        String token = bundle.getString("token");
        isActivityForResult = false;
        if ("chat_frag".equals(bundle.getString("from_activity"))) {
            isActivityForResult = true;
        }

        Log.e(LOG_TAG, "Type: " + type + " From: " + from + " To: " + to + " Token: " + token);

        fetchFriendNameAndPic(from);

        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        audioPlayer = new AudioPlayer(this);
        audioPlayer.playRingtone();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.nuggetchat.messenger.DISMISS_INCOMING_CALL_ACTIVITY")) {
                    unregisterReceiver(this);
                    ((NuggetApplication)getApplication()).setIncomingCall(false);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("com.nuggetchat.messenger.DISMISS_INCOMING_CALL_ACTIVITY");
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void fetchFriendNameAndPic(final String from) {
        String firebaseUri = Conf.firebaseUsersUri();
        Log.i(LOG_TAG, "firebaseURI, " + firebaseUri);
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getValue());
                UserInfo  userInfo = dataSnapshot.getValue(UserInfo.class);
               if (userInfo.getFacebookId().equals(from)) {
                   String userName = userInfo.getName();
                   callerName.setText(userName);
                   final String callerPic = getPicForCaller(userInfo.getFacebookId());
                   Glide.with(getApplicationContext()).load(callerPic).asBitmap().centerCrop().into(new BitmapImageViewTarget(callerImage) {
                       @Override
                       protected void setResource(Bitmap resource) {
                           RoundedBitmapDrawable circularBitmapDrawable =
                                   RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), resource);
                           circularBitmapDrawable.setCircular(true);
                           callerImage.setImageDrawable(circularBitmapDrawable);
                       }
                   });
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

    private String getPicForCaller(String facebookUserId) {
        return "https://graph.facebook.com/" + facebookUserId + "/picture?width=200&height=150";
    }


    @OnClick(R.id.accept_btn)
    public void acceptButtonClick(){
        ((NuggetApplication)getApplication()).setIncomingCall(false);
        triggerUserAction(true /*accepted*/);
    }

    @OnClick(R.id.reject_btn)
    public void rejectButtonClick(){
        ((NuggetApplication)getApplication()).setIncomingCall(false);
        triggerUserAction(false /*accepted*/);
    }

    private void triggerUserAction(boolean accepted) {
        audioPlayer.stopRingtone();
        if (!isActivityForResult) {
            Log.i(LOG_TAG, "MessageHandler Trigger - Start game chat activity ");
            Intent startChatIntent = new Intent(this, GamesChatActivity.class);
            bundle.putBoolean(CALL_ACCEPTED, accepted);
            startChatIntent.putExtras(bundle);
            startActivity(startChatIntent);
        } else {
            Log.i(LOG_TAG, "MessageHandler Trigger - Restart game chat activity ");
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
            Log.e(LOG_TAG, "Exception while unregistering");
        }
        super.onPause();
    }
}
