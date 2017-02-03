package com.nuggetchat.messenger.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.chat.IncomingCallActivity;
import com.nuggetchat.messenger.utils.MyLog;

public class MessagingService extends GcmListenerService {

    private static final String LOG_TAG = MessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(String from, Bundle data){
        super.onMessageReceived(from, data);
        MyLog.i(LOG_TAG, "New Message received");
        if (data != null && data.size() > 0) {
            MyLog.i(LOG_TAG, "Received data" + data);
            String dataType = Utils.getCleanStr(data.getString("type"));
            if(data.containsKey("mp_message")) {
                NuggetInjector.getInstance().getMixpanel().handleMixpanelMesg(data);
            } else if ("pre_call_handshake".equals(dataType)) {
                triggerIncomingCall(data, dataType);
            } else {
                MyLog.w(LOG_TAG, "Unknown message data :" + data);
            }
        }
    }

    private void triggerIncomingCall(Bundle data, String dataType) {
        // invoke incoming call activity
        Intent intent = new Intent(this, IncomingCallActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("type", dataType);
        bundle.putString("from", data.getString("sender"));
        bundle.putString("to", data.getString("receiver"));
        bundle.putString("token", data.getString("token"));
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_FROM_BACKGROUND
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        try {
            MyLog.i(LOG_TAG, "Incoming call activity triggered");
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            MyLog.w(LOG_TAG, "Unable to start Incomming call ", e);
        }
    }
}
