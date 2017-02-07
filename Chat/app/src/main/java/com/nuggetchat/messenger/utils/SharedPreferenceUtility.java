package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.BuildConfig;

public class SharedPreferenceUtility {
    private static final String PREF_FILE_NAME = BuildConfig.APPLICATION_ID + ".common.pref";
    private static final String FACEBOOK_USER_ID = "facebook_user_id";
    private static final String FACEBOOK_USER_NAME = "facebook_username";
    private static final String ICE_SERVERS_STRING = "ice_servers";
    private static final String FACEBOOK_ACCESS_TOKEN = "facebook_access_token";
    private static final String FIREBASE_ID_TOKEN = "firebase_id_token";
    private static final String FIREBASE_UID = "firebase_uid";
    private static final String FAV_FRIEND_1 = "fav_friend_1";
    private static final String FAV_FRIEND_2 = "fav_friend_2";
    private static final String NUMBER_OF_FRIENDS = "number_of_friends";
    private static final String MIXPANEL_USER_DETAIL_UPDATED = "mixpanel_userdetail_updated";
    private static final String DEVICE_TOKEN_PUSHED_TO_SERVER = "device_token_pushed_to_server_v1";
    private static final String INSTALL_REFERRER = "install_referrer";
    private static final String APPS_FLYER_CONV_DATA = "apps_flyer_conv_data";

    public static int getNumberOfFriends(Context context) {
        return getPreferences(context).getInt(NUMBER_OF_FRIENDS,0);
    }

    public static void setNumberOfFriends(int numberOfFriends, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(NUMBER_OF_FRIENDS,numberOfFriends);
        editor.apply();
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_FILE_NAME, Context.MODE_PRIVATE);

        return sharedPref.edit();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    private static void setPreference(Context context, String key, String value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(key, value);
        editor.apply();
    }

    private static void setPreference(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setFacebookUserId(String facebookUserId, Context context){
        setPreference(context, FACEBOOK_USER_ID, facebookUserId);
    }

    public static String getFacebookUserId(Context context){
        return getPreferences(context).getString(FACEBOOK_USER_ID, "");
    }

    public static void setFacebookUserName(String facebookUserName, Context context) {
        setPreference(context, FACEBOOK_USER_NAME, facebookUserName);
    }

    public static String getFacebookUserName(Context context) {
        return getPreferences(context).getString(FACEBOOK_USER_NAME,"");
    }

    public static void setIceServersUrls(String iceServers, Context context) {
        setPreference(context, ICE_SERVERS_STRING, iceServers);
    }

    public static String getIceServersUrls(Context context) {
        return getPreferences(context).getString(ICE_SERVERS_STRING, "");
    }

    public static void setFacebookAccessToken(String facebookAccessToken, Context context) {
        setPreference(context, FACEBOOK_ACCESS_TOKEN, facebookAccessToken);
    }

    public static String getFacebookAccessToken(Context context) {
        return getPreferences(context).getString(FACEBOOK_ACCESS_TOKEN,"");
    }

    public static void setFirebaseIdToken(String firebaseIdToken, Context context) {
        setPreference(context, FIREBASE_ID_TOKEN, firebaseIdToken);
    }

    public static String getFirebaseIdToken(Context context) {
        return getPreferences(context).getString(FIREBASE_ID_TOKEN,"");
    }

    public static void setFirebaseUid(String firebaseUid, Context context) {
        setPreference(context, FIREBASE_UID, firebaseUid);
    }

    public static String getFirebaseUid(Context context) {
        return getPreferences(context).getString(FIREBASE_UID,"");
    }

    public static void setUserLoggedInMixpanel(Context context) {
        setPreference(context, MIXPANEL_USER_DETAIL_UPDATED, true);
    }

    public static boolean isUserLoggedInMixpanel(Context context) {
        return getPreferences(context).getBoolean(MIXPANEL_USER_DETAIL_UPDATED, false);
    }


    public static void setDeviceTokenPushedToServer(Context context) {
        setPreference(context, DEVICE_TOKEN_PUSHED_TO_SERVER, true);
    }

    public static boolean isDeviceTokenPushedToServer(Context context) {
        return getPreferences(context).getBoolean(DEVICE_TOKEN_PUSHED_TO_SERVER, false);
    }

    public static void setFavFriend1(String favFriend1, Context context) {
        setPreference(context, FAV_FRIEND_1, favFriend1);
    }

    public static String getFavFriend1(Context context) {
        return getPreferences(context).getString(FAV_FRIEND_1,"");
    }

    public static void setFavFriend2(String favFriend2, Context context) {
        setPreference(context, FAV_FRIEND_2, favFriend2);
    }

    public static String getFavFriend2(Context context) {
        return getPreferences(context).getString(FAV_FRIEND_2,"");
    }

    public static void setFavouriteFriend(Context context, String favFriend) {
        if (getFavFriend1(context).equals("")) {
            setFavFriend1(favFriend, context);
        } else {
            if (!getFavFriend1(context).equals(favFriend)) {
                setFavFriend2(getFavFriend1(context), context);
                setFavFriend1(favFriend, context);
            }
        }
    }

    public static String getInstallReferrer(Context context) {
        return getPreferences(context).getString(INSTALL_REFERRER, "");
    }

    public static void setInstallReferrer(Context context, String dataReceived) {
        setPreference(context, INSTALL_REFERRER, dataReceived);
    }


    /**
     * Retrieves appsflyers conversion data received in a deep link, or install
     *
     * @param context application context
     * @param key check for available keys here
     *            https://support.appsflyer.com/hc/en-us/articles/207032096-Accessing-AppsFlyer-Attribution-Conversion-Data-from-the-SDK-iOS-Deferred-Deeplinking-
     * @return string stored for each attribution key
     */
    public static String getAppsFlyerConversionData(Context context, String key) {
        if (Utils.isNullOrEmpty(key)) {
            return "";
        }
        return getPreferences(context).getString(APPS_FLYER_CONV_DATA + "_" + key, "");
    }

    public static void setAppsFlyerConversionData(Context context, String key, String value) {
        if (Utils.isNullOrEmpty(key) || Utils.isNullOrEmpty(value)) {
            return;
        }
        setPreference(context, APPS_FLYER_CONV_DATA + "_" + key, value);
    }
}
