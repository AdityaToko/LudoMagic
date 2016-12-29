package com.nuggetchat.messenger;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class NuggetApplication extends Application {
    private static boolean initialized = false;
    private boolean isInitiator = false;
    private boolean isInCall = false;
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

    public boolean isInCall() {
        return isInCall;
    }

    public void setInCall(boolean inCall) {
        isInCall = inCall;
    }
}
