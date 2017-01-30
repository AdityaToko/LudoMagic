package com.nuggetchat.messenger;

import android.media.AudioManager;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.messenger.utils.MyLog;

public class NuggetApplication extends MultiDexApplication {
    private static boolean initialized = false;
    private static final String LOG_TAG = NuggetApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        handleUncaughtExceptions();
        if (initialized) {
            MyLog.w(LOG_TAG, "Activity initialized again.");
            return;
        }

        initialized = true;

        if (!BuildConfig.DEBUG) {
            FacebookSdk.sdkInitialize(getApplicationContext());
            AppEventsLogger.activateApp(this);
            FacebookSdk.setIsDebugEnabled(false);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS);
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, FirebaseOptions.fromResource(this));
        }
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        NuggetInjector.getInstance().setAppContext(this);
    }

    private void handleUncaughtExceptions() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                MyLog.e(LOG_TAG, "Uncaught exception", ex);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(false);
            }
        });
    }
}
