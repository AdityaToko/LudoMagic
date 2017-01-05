package com.nuggetchat.messenger.chat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.activities.GamesChatActivity;
import com.nuggetchat.messenger.utils.GlideUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IncomingCallActivity extends AppCompatActivity {
    private static final String LOG_TAG = IncomingCallActivity.class.getSimpleName();
    @BindView(R.id.accept_btn)
    public Button acceptButton;
    @BindView(R.id.reject_btn)
    public Button rejectButton;
    @BindView(R.id.caller_name_txt)
    public TextView callerName;
    @BindView(R.id.caller_image)
    public ImageView callerImage;

    Bundle bundle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        bundle = intent.getExtras();

        String type = bundle.getString("type");
        String from = bundle.getString("from");
        String to = bundle.getString("to");
        String token = bundle.getString("token");

        Log.i(LOG_TAG, "facebook id of friend, " + from);
        Log.e(LOG_TAG, "Type: " + type + " From: " + from + " To: " + to + " Token: " + token);

        fetchFriendNameAndPic(from);

        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void fetchFriendNameAndPic(final String from) {
        String firebaseUri = Conf.firebaseUsersURI();
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
                   String callerPic = getPicForCaller(userInfo.getFacebookId());
                   GlideUtils.loadImage(getApplicationContext(),callerImage , null, callerPic);
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
        Intent startChatIntent = new Intent(this, GamesChatActivity.class);
        startChatIntent.putExtras(bundle);
        startActivity(startChatIntent);
        finish();
    }

    @OnClick(R.id.reject_btn)
    public void rejectButtonClick(){
        finishAffinity();
    }
}
