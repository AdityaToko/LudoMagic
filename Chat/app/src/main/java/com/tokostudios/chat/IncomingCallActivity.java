package com.tokostudios.chat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;

import com.nuggetchat.messenger.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IncomingCallActivity extends AppCompatActivity {

    @BindView(R.id.accept_btn)
    Button acceptButton;

    @BindView(R.id.reject_btn)
    Button rejectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }


    @OnClick(R.id.accept_btn)
    void acceptButtonClick(){
        startActivity(new Intent(this, ChatActivity.class));
        finish();
    }

    @OnClick(R.id.reject_btn)
    void rejectButtonClick(){
        finish();
    }
}
