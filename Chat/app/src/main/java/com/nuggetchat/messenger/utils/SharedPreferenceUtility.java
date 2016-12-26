package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.nuggetchat.messenger.BuildConfig;

public class SharedPreferenceUtility {
    private static final String PREF_FILE_NAME = BuildConfig.APPLICATION_ID + ".common.pref";
    private final static String LOG_TAG = SharedPreferenceUtility.class.getSimpleName();
    private static final String FACEBOOK_USER_ID = "facebook_user_id";
    private Context context;

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_FILE_NAME, Context.MODE_PRIVATE);

        return sharedPref.edit();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void setFacebookUserId(String facebookUserId, Context context){
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FACEBOOK_USER_ID, facebookUserId);
        editor.apply();
    }

    public static String getFacebookUserId(Context context){
        return getPreferences(context).getString(FACEBOOK_USER_ID, "");
    }
}
