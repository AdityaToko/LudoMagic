package com.nuggetchat.messenger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mixpanel.android.mpmetrics.InstallReferrerReceiver;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

/**
 * Handling Install referrer and sending to multiple locations
 *
 * To test if this will get called do following command
 * {@code
 *  adb shell am  broadcast -a com.android.vending.INSTALL_REFERRER  --es "referrer" "some-referrer-details"  com.nuggetmessenger.games.debug
 * }
 *
 * To test if this Receiver try passing -n param in above command eg:
 * <pre>
 * {@code
 *   adb shell am  broadcast -a com.android.vending.INSTALL_REFERRER -n com.nuggetmessenger.games.debug/com.nuggetchat.messenger.receivers.ManyInstallTrackersReceiver --es "referrer" "some-referrer-details"
 *  }
 * </pre>
 *
 */

public class ManyInstallTrackersReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = ManyInstallTrackersReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "Install Receiver");

        /**
         * Sending Install referrer to Mixpanel
         */
        InstallReferrerReceiver mixpanelReferrerTracking = new InstallReferrerReceiver();
        mixpanelReferrerTracking.onReceive(context, intent);

        /**
         * Saving referrer in shared pref
         */
        Bundle bundle = intent.getExtras();
        StringBuilder installData = new StringBuilder();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    installData.append(String.format("%s\t%s\t%s;", key,
                            value.toString(), value.getClass().getName()));
                }
            }
        }
        String dataReceived = Utils.getCleanStr(installData.toString());
        Log.i(LOG_TAG, "Install receiver called " + dataReceived);
        SharedPreferenceUtility.setInstallReferrer(context, dataReceived);
    }
}
