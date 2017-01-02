package com.nuggetchat.messenger;

import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class NuggetApplication extends MultiDexApplication {
    private static boolean initialized = false;
    private boolean isInitiator = false;
    private boolean isOngoingCall = false;
    @Override
    public void onCreate() {
        super.onCreate();

        if (initialized) {
            return;
        }

        initialized = true;
        FacebookSdk.sdkInitialize(getApplicationContext());
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, FirebaseOptions.fromResource(this));
        }
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        AppEventsLogger.activateApp(this);
    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public void setInitiator(boolean initiator) {
        isInitiator = initiator;
    }

    public boolean isOngoingCall() {
        return isOngoingCall;
    }

    public void setOngoingCall(boolean ongoingCall) {
        isOngoingCall = ongoingCall;
    }
}
