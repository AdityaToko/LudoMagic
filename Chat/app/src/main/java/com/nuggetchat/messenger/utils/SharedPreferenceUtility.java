package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;

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

    public static void setFacebookUserId(String facebookUserId, Context context){
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FACEBOOK_USER_ID, facebookUserId);
        editor.apply();
    }

    public static String getFacebookUserId(Context context){
        return getPreferences(context).getString(FACEBOOK_USER_ID, "");
    }

    public static void setFacebookUserName(String facebookUserName, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FACEBOOK_USER_NAME, facebookUserName);
        editor.apply();
    }

    public static String getFacebookUserName(Context context) {
        return getPreferences(context).getString(FACEBOOK_USER_NAME,"");
    }

    public static void setIceServersUrls(String iceServers, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(ICE_SERVERS_STRING, iceServers);
        editor.apply();
    }

    public static String getIceServersUrls(Context context) {
        return getPreferences(context).getString(ICE_SERVERS_STRING, "");
    }

    public static void setFacebookAccessToken(String facebookAccessToken, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FACEBOOK_ACCESS_TOKEN, facebookAccessToken);
        editor.apply();
    }

    public static String getFacebookAccessToken(Context context) {
        return getPreferences(context).getString(FACEBOOK_ACCESS_TOKEN,"");
    }

    public static void setFirebaseIdToken(String firebaseIdToken, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FIREBASE_ID_TOKEN, firebaseIdToken);
        editor.apply();
    }

    public static String getFirebaseIdToken(Context context) {
        return getPreferences(context).getString(FIREBASE_ID_TOKEN,"");
    }

    public static void setFirebaseUid(String firebaseUid, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FIREBASE_UID, firebaseUid);
        editor.apply();
    }

    public static String getFirebaseUid(Context context) {
        return getPreferences(context).getString(FIREBASE_UID,"");
    }

    public static void setUserLoggedInMixpanel(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(MIXPANEL_USER_DETAIL_UPDATED, true);
        editor.apply();
    }

    public static boolean isUserLoggedInMixpanel(Context context) {
        return getPreferences(context).getBoolean(MIXPANEL_USER_DETAIL_UPDATED, false);
    }


    public static void setDeviceTokenPushedToServer(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(DEVICE_TOKEN_PUSHED_TO_SERVER, true);
        editor.apply();
    }

    public static boolean isDeviceTokenPushedToServer(Context context) {
        return getPreferences(context).getBoolean(DEVICE_TOKEN_PUSHED_TO_SERVER, false);
    }

    public static void setFavFriend1(String favFriend1, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FAV_FRIEND_1, favFriend1);
        editor.apply();
    }

    public static String getFavFriend1(Context context) {
        return getPreferences(context).getString(FAV_FRIEND_1,"");
    }

    public static void setFavFriend2(String favFriend2, Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(FAV_FRIEND_2, favFriend2);
        editor.apply();
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
}
