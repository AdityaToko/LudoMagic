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
    private static final String LOG_TAG = NuggetApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        handleUncaughtExceptions();
        if (initialized) {
            Log.w(LOG_TAG, "Activity initialized again.");
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
                Log.e(LOG_TAG, "Uncaught exception", ex);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(false);
            }
        });
    }
}
