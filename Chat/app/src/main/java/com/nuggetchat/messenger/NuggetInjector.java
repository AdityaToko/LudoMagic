package com.nuggetchat.messenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class NuggetInjector {
    @SuppressLint("StaticFieldLeak")
    private static NuggetInjector nuggetInjector;

    private FirebaseAnalytics firebaseAnalytics;
    private Context appContext;
    private boolean isInitiator = false;
    private boolean isOngoingCall = false;
    private boolean isIncomingCall = false;

    private NuggetInjector() {
    }
    public void setAppContext(Context appContext) {
        nuggetInjector.appContext = appContext;
        firebaseAnalytics = FirebaseAnalytics.getInstance(appContext);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
    }
    public Context getAppContext() {
        return appContext;
    }


    public static NuggetInjector getInstance() {
        if (nuggetInjector == null) {
            nuggetInjector = new NuggetInjector();
        }
        return nuggetInjector;
    }
    public void logEvent(String event, Bundle bundle) {
        firebaseAnalytics.logEvent(event, bundle);
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

    public boolean isIncomingCall() {
        return isIncomingCall;
    }

    public void setIncomingCall(boolean incomingCall) {
        isIncomingCall = incomingCall;
    }
}
