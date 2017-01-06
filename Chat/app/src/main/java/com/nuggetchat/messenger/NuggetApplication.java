package com.nuggetchat.messenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.FirebaseDatabase;

public class NuggetApplication extends MultiDexApplication {
    @SuppressLint("StaticFieldLeak")
    private static NuggetApplication nuggetApplication;
    private FirebaseAnalytics firebaseAnalytics;

    private static boolean initialized = false;
    private boolean isInitiator = false;
    private boolean isOngoingCall = false;
    @Override
    public void onCreate() {
        super.onCreate();

        handleUncaughtExceptions();

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

    public static NuggetApplication getInstance() {
        if (nuggetApplication == null) {
            nuggetApplication = new NuggetApplication();
        }
        return nuggetApplication;
    }

    public void logEvent(Context appContext, String event, Bundle bundle) {
        firebaseAnalytics= FirebaseAnalytics.getInstance(appContext);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        firebaseAnalytics.logEvent(event, bundle);
    }

    private void handleUncaughtExceptions() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e("MyApplication", ex.getMessage());
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(false);
            }
        });
    }
}
