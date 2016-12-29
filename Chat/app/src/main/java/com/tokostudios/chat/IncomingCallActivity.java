package com.tokostudios.chat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;

import com.nuggetchat.messenger.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IncomingCallActivity extends AppCompatActivity {
    private static final String LOG_TAG = IncomingCallActivity.class.getSimpleName();
    @BindView(R.id.accept_btn)
    Button acceptButton;

    @BindView(R.id.reject_btn)
    Button rejectButton;
    Bundle bundle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        bundle = intent.getExtras();
        String from = bundle.getString("from");
        String to = bundle.getString("to");
        String type = bundle.getString("type");
        String sdp = bundle.getString("sdp");
        Log.e(LOG_TAG, from + " " + to + " " + " " + type + " " + sdp);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }


    @OnClick(R.id.accept_btn)
    void acceptButtonClick(){
        Intent startChatIntent = new Intent(this, ChatActivity.class);
        startChatIntent.putExtras(bundle);
        startActivity(startChatIntent);
        finish();
    }

    @OnClick(R.id.reject_btn)
    void rejectButtonClick(){
        finishAffinity();
    }
}
