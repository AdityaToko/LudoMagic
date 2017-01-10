package com.nuggetchat.messenger;

import android.media.AudioManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class NuggetApplication extends MultiDexApplication {
    private static boolean initialized = false;

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
        NuggetInjector.getInstance().setAppContext(this);
        AppEventsLogger.activateApp(this);
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
