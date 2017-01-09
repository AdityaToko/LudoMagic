package com.nuggetchat.messenger;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class NuggetInjector {
    private static NuggetInjector nuggetInjector;


    public static NuggetInjector getInstance() {
        if (nuggetInjector == null) {
            nuggetInjector = new NuggetInjector();
        }
        return nuggetInjector;
    }
    public void logEvent(Context appContext, String event, Bundle bundle) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(appContext);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        firebaseAnalytics.logEvent(event, bundle);
    }

}
