package com.nuggetchat.messenger.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.activities.GamesChatActivity;

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
        String from = bundle.getString("from");
        Log.i(LOG_TAG, "facebook id of friend, " + from);
        fetchFriendNameAndPic(from);
        String to = bundle.getString("to");
        String type = bundle.getString("type");
        String sdp = bundle.getString("sdp");
        Log.e(LOG_TAG, from + " " + to + " " + " " + type + " " + sdp);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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
