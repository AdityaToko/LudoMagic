package com.nuggetchat.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.nuggetchat.messenger.chat.IncomingCallActivity;

public class CallRequestReceiver extends WakefulBroadcastReceiver {
    public CallRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("com.nuggetchat.messenger.intent.action.INCOMING_CALL")){
            Intent receiveCallIntent = new Intent(context, IncomingCallActivity.class);
            Bundle bundle = intent.getExtras();
            receiveCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            receiveCallIntent.putExtras(bundle);
            context.startActivity(receiveCallIntent);
        }
    }
}
