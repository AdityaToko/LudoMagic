package com.nuggetchat.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nuggetchat.messenger.chat.ChatService;

public class RestartServiceReceiver extends BroadcastReceiver {
    public RestartServiceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
       if(intent.getAction().equals("com.android.ServiceStopped")){
           context.startService(new Intent(context, ChatService.class));
       }
    }
}
