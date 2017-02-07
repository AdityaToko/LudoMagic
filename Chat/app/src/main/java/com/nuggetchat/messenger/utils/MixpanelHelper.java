package com.nuggetchat.messenger.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.R;

import org.json.JSONException;
import org.json.JSONObject;

public class MixpanelHelper {

    private static final String MIXPANEL_TOKEN = "28ffd6fc1e328bf0213b8bf9ff2af0ca";
    private static final String LOG_TAG = MixpanelHelper.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static MixpanelHelper mixpanelHelper = null;
    private MixpanelAPI mixpanel;
    private String gcmSenderId;
    private Context appContext;

    private MixpanelHelper(Context appContext, String gcmSenderId) {
        mixpanel = MixpanelAPI.getInstance(appContext, MIXPANEL_TOKEN);
        this.gcmSenderId = gcmSenderId;
        this.appContext = appContext;
    }

    public static MixpanelHelper getInstance(Context appContext) {
        if (mixpanelHelper == null) {
            String gcmSenderId = appContext.getString(R.string.gcm_defaultSenderId);
            mixpanelHelper = new MixpanelHelper(appContext, gcmSenderId);
        }
        return mixpanelHelper;
    }

    public void handleMixpanelMesg(Bundle data) {
        MyLog.i(LOG_TAG, "Received message from Mixpanel"
                + Utils.getCleanStr(data.getString("mp_message")));
    }

    /**
     * Call identify with Alias id
     */
    public void identifyUser() {
        String userId = SharedPreferenceUtility.getFirebaseUid(appContext);
        if (!Utils.isNullOrEmpty(userId)) {
            Log.i(LOG_TAG, "Identifying user " + userId);
            mixpanel.identify(userId);
            mixpanel.getPeople().identify(userId);
        }
    }

    /**
     * Returns distinct id might be one id before install and after Identify is called
     * @return
     */
    public String getUserId() {
        return mixpanel.getDistinctId();
    }


    public void loginUserAndUpdateUserDetails(String firebaseUid) {
        if(SharedPreferenceUtility.isUserLoggedInMixpanel(appContext)) {
            Log.d(LOG_TAG, "User detail already updated in mixpanel");
            return;
        }
        if (Utils.isNullOrEmpty(firebaseUid)) {
            Log.w(LOG_TAG, "User id not yet set");
            return;
        }
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(), new OnCompleteUpdateMixpanel(firebaseUid));
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,email,age_range");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public void flush() {
        MyLog.i(LOG_TAG, "Flushing");
        mixpanel.flush();
    }

    public void startTimer(String timerName) {
        mixpanel.timeEvent(timerName);
    }

    public void stopTimer(String timerName) {
        mixpanel.track(timerName);
    }

    public void track(String eventName, String eventValue) {
        try {
            MyLog.i(LOG_TAG, "Tracking event in mixpanel");
            JSONObject props = new JSONObject();
            props.put("value", eventValue);
            mixpanel.track(eventName, props);
        } catch (JSONException e) {
            MyLog.w(LOG_TAG, "Exception tracking mixpanel event ", e);
        }
    }

    public void logCreateView(String activityClass) {
        track(AnalyticConstants.ON_CREATE_ACTIVITY, activityClass);
    }


    public void logShowView(String logTag) {
        track(AnalyticConstants.ON_SHOW_VIEW, logTag);
    }

    public void trackError(String logTag, String errorMessage) {
        trackError(logTag, errorMessage, null /* exception */);
    }

    public void trackError(String logTag, String errorMessage, Exception e) {
        try {
            JSONObject props = new JSONObject();
            props.put("tag", logTag);
            props.put("error", errorMessage);
            if (e != null) {
                props.put("exception", Utils.getStackTrace(e));
            }
            mixpanel.track(AnalyticConstants.APP_ERROR, props);
        } catch (JSONException je) {
            MyLog.w(LOG_TAG, "Exception tracking mixpanel event ", je);
        }
    }

    /**
     * Using data available in Facebook update user information in mixpanel
     * TODO add gender from https://market.mashape.com/simsik/detect-gender-by-name
     * user account ajitsen@tokostudios.com
     */
    private class OnCompleteUpdateMixpanel implements GraphRequest.GraphJSONObjectCallback {

        private String userId;
        public OnCompleteUpdateMixpanel(String userId) {
            this.userId = userId;
        }

        @Override
        public void onCompleted(JSONObject object, GraphResponse response) {
            Log.i(LOG_TAG, "Facebook user details response " + response.toString());
            JSONObject props = new JSONObject();

            String email = "";
            String name = "";
            int minAge = 0;
            int maxAge = 0;
            try {
                email = object.getString("email");
                if (!Utils.isNullOrEmpty(email)) {
                    props.put("$email", email);
                }
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing email ", e);
            }
            try {
                name = object.getString("name");
                if (!Utils.isNullOrEmpty(name)) {
                    props.put("$name", name);
                }
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing name ", e);
            }
            try{
                minAge = object.getJSONObject("age_range").getInt("min");
                if (minAge > 0) {
                    props.put("min_age", minAge);
                }
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing minAge ", e);
            }
            try{
                maxAge = object.getJSONObject("age_range").getInt("max");
                props.put("max_age", maxAge);
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing maxAge ", e);
            }
            Log.i(LOG_TAG, "Email: " + email + " name: " + name +" minAge:"
                    + minAge +" maxAge:" + maxAge);

            // https://mixpanel.com/help/reference/android#identify
            // Not using alias since its recommended to be called only
            // once during lifetime of user - eg sign-up (not login)
            mixpanel.identify(userId);

            MixpanelAPI.People people = mixpanel.getPeople();
            // Identify people data
            people.identify(userId);

            Log.i(LOG_TAG, "Enabled push notification for user");
            people.initPushHandling(gcmSenderId);
            if (props.length() > 0) {
                people.set(props);
            }
            Log.i(LOG_TAG, "User detail updated");
            SharedPreferenceUtility.setUserLoggedInMixpanel(appContext);
            flush();
        }
    }

    /**
     * https://mixpanel.com/help/reference/android-push-notifications#advanced
     * @param gcmToken GCM token
     */
    public void setupPushHandling(String gcmToken) {

        String userId = SharedPreferenceUtility.getFirebaseUid(appContext);

        MixpanelAPI.People people = mixpanel.getPeople();
        // Identify people data
        people.identify(userId);

        Log.i(LOG_TAG, "Enabled push notification for user");
        people.setPushRegistrationId(gcmToken);
    }
}

