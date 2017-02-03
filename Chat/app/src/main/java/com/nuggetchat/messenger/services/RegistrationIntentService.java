package com.nuggetchat.messenger.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.nuggetchat.messenger.utils.GcmDeviceTokenUtils;


public class RegistrationIntentService extends IntentService {

    private static final String LOG_TAG = RegistrationIntentService.class.getSimpleName();

    public RegistrationIntentService() {
        super(LOG_TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "Intent received to update token");
        GcmDeviceTokenUtils.refreshDeviceRegTokens();
    }
}
