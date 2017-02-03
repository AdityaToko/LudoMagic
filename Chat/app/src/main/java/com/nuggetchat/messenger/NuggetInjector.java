package com.nuggetchat.messenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.nuggetchat.messenger.utils.MixpanelHelper;

public class NuggetInjector {
    @SuppressLint("StaticFieldLeak")
    private static NuggetInjector nuggetInjector;

    private FirebaseAnalytics firebaseAnalytics;
    private MixpanelHelper mixpanelHelper;
    private Context appContext;
    private boolean isInitiator = false;
    private boolean isOngoingCall = false;
    private boolean isIncomingCall = false;
    private boolean isChatServiceRunning = false;
    private int screenLandscapeWidth;
    private int screenLandscapeHeight;


    private NuggetInjector() {
    }
    public void setAppContext(Context appContext) {
        nuggetInjector.appContext = appContext;
        firebaseAnalytics = FirebaseAnalytics.getInstance(appContext);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        mixpanelHelper = MixpanelHelper.getInstance(appContext);
    }

    public Context getAppContext() {
        return appContext;
    }

    public String getString(int resourceId) {
        return appContext.getString(resourceId);
    }


    public static NuggetInjector getInstance() {
        if (nuggetInjector == null) {
            nuggetInjector = new NuggetInjector();
        }
        return nuggetInjector;
    }

    public Resources getResources() {
        return appContext.getResources();
    }

    public void logEvent(String event, Bundle bundle) {
        firebaseAnalytics.logEvent(event, bundle);
    }

    public MixpanelHelper getMixpanel() {
        return mixpanelHelper;
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

    public boolean isChatServiceRunning(){
        return isChatServiceRunning;
    }

    public void setChatServiceRunning(boolean chatServiceRunning){
        isChatServiceRunning = chatServiceRunning;
    }

    public int getScreenLandscapeWidth() {
        if (screenLandscapeWidth == -1) {
            calculateScreenSize();
        }

        return screenLandscapeWidth;
    }

    public int getScreenLandscapeHeight() {
        if (screenLandscapeHeight == -1) {
            calculateScreenSize();
        }

        return screenLandscapeHeight;
    }

    private void calculateScreenSize() {
        final DisplayMetrics metrics = new DisplayMetrics();
        Display display = ((WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        if (android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(metrics);
            screenLandscapeWidth = metrics.widthPixels;
            screenLandscapeHeight = metrics.heightPixels;
        } else {
            try {
                screenLandscapeWidth = (Integer) Display.class.getMethod("getRawWidth")
                        .invoke(display);
                screenLandscapeHeight = (Integer) Display.class.getMethod("getRawHeight")
                        .invoke(display);
            } catch (Exception e) {
                screenLandscapeWidth = appContext.getResources().getDisplayMetrics().widthPixels;
                screenLandscapeHeight = appContext.getResources().getDisplayMetrics().heightPixels;
            }
        }

        // Swap values if queried in portrait mode.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            int temp = screenLandscapeWidth;
            // noinspection SuspiciousNameCombination Swapping width and height
            screenLandscapeWidth = screenLandscapeHeight;
            screenLandscapeHeight = temp;
        }
    }
}
