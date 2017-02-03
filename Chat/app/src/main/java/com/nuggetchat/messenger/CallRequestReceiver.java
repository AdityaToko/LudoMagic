package com.nuggetchat.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.nuggetchat.messenger.chat.IncomingCallActivity;

public class CallRequestReceiver extends WakefulBroadcastReceiver {
    private static final String LOG_TAG = CallRequestReceiver.class.getSimpleName();

    public CallRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("com.nuggetchat.messenger.intent.action.INCOMING_CALL")){
            Log.i(LOG_TAG, "Received call request");
            Intent receiveCallIntent = new Intent(context, IncomingCallActivity.class);
            Bundle bundle = intent.getExtras();
            receiveCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            receiveCallIntent.putExtras(bundle);
            context.startActivity(receiveCallIntent);
        } else {
            Log.i(LOG_TAG, "Unsupported action " + intent.getAction());
        }
    }
}