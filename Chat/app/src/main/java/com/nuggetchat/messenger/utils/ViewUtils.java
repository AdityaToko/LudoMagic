package com.nuggetchat.messenger.utils;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.appsee.Appsee;
import com.nuggetchat.messenger.BuildConfig;

public class ViewUtils {

    public static void setWindowImmersive(Window window) {
        if (window != null) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static void setWindowImmersive(final Window window, Handler uiHandler) {
        if (uiHandler != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    setWindowImmersive(window);
                }
            });
        }
    }

    public static void showWindowNavigation(Window window) {
        if (window != null) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public static void showWindowNavigation(final Window window, Handler uiHandler) {
        if (uiHandler != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showWindowNavigation(window);
                }
            });
        }
    }

    public static String getInviteSubject() {
        return "Let's play a game! Try out Nugget Games with me";
    }

    public static String getInviteBody() {
        String appId = "https://play.google.com/store/apps/details?id=com.nuggetmessenger.games";
        return "Hey! Found this app where we can play multiplayer games while " +
                "voice-calling! Install it from " + appId;
    }

    public static void hideViewsFromAppsee(View view, String logTag) {
        MyLog.i(logTag, "hiding a view from Appsee");
        Appsee.markViewAsSensitive(view);
    }

    public static void startAppseeAnalytics(String appSeeId, String logTag) {
        /**
         * Starting Appsee for User analytics
         */
        if (BuildConfig.DEBUG) {
            Log.i(logTag, "Starting Appsee in debug");
            Appsee.start(appSeeId);
        } else {
            Log.i(logTag, "Appsee not enabled on production");
        }
    }
}
