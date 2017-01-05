package com.nuggetchat.messenger;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nuggetchat.messenger.chat.IncomingCallActivity;

import org.json.JSONObject;

import java.util.Map;

public class FBMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = "FBMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(LOG_TAG, "FBMessagingService onMessageReceived() invoked!");

        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(LOG_TAG, "Received data" + data);

            if (data.containsKey("type") && "pre_call_handshake".equals(data.get("type"))) {
                // invoke incoming call activity
                Intent intent = new Intent(this, IncomingCallActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("type", data.get("type"));
                bundle.putString("from", data.get("sender"));
                bundle.putString("to", data.get("receiver"));
                bundle.putString("token", data.get("token"));
                intent.putExtras(bundle);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_FROM_BACKGROUND
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                try {
                    // Perform the operation associated with our pendingIntent
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
