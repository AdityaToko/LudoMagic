package com.nuggetchat.messenger.utils;

import android.util.Log;

import com.nuggetchat.messenger.BuildConfig;

public class MyLog {

    private MyLog() {}

    public static void v (String log, String message) {
        if (BuildConfig.DEBUG) {
            Log.v(log, message);
        }
    }

    public static void v (String log, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.v(log, message, throwable);
        }
    }

    public static void d (String log, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(log, message);
        }
    }

    public static void d (String log, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.i(log, message, throwable);
        }
    }


    public static void i (String log, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(log, message);
        }
    }

    public static void i (String log, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.i(log, message, throwable);
        }
    }

    public static void w (String log, String message) {
        if (BuildConfig.DEBUG) {
            Log.w(log, message);
        }
    }

    public static void w (String log, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(log, message, throwable);
        }
    }


    public static void e (String log, String message) {
        if (BuildConfig.DEBUG) {
            Log.e(log, message);
        }
    }

    public static void e (String log, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(log, message, throwable);
        }
    }

}
