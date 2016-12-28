package com.nuggetchat.messenger;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.tokostudios.chat.ChatActivity;

public class CallRequestReceiver extends WakefulBroadcastReceiver {
    public CallRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("com.nuggetchat.messenger.intent.action.INCOMING_CALL")){
            Intent receiveCallIntent = new Intent(context, ChatActivity.class);
            receiveCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(receiveCallIntent);
        }
    }
}
