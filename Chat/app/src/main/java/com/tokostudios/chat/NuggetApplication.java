package com.tokostudios.chat;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class NuggetApplication extends Application {
    private static boolean initialized = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (initialized) {
            return;
        }

        initialized = true;
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }
}
