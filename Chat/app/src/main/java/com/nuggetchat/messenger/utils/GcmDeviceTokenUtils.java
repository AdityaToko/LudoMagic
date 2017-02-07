package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.base.NuggetInjector;
import com.nuggetchat.messenger.datamodel.CloudMesgToken;

import java.io.IOException;

import static com.nuggetchat.lib.Conf.FACEBOOK_DEVICE_TOKEN;
import static com.nuggetchat.lib.Conf.FIREBASE_DEVICE_TOKEN;
import static com.nuggetchat.lib.Conf.facebookDeviceToken;
import static com.nuggetchat.lib.Conf.firebaseDeviceToken;

public class GcmDeviceTokenUtils {
    public static final String LOG_TAG = GcmDeviceTokenUtils.class.getSimpleName();
    private static final String CLOUD_MSG_TOKEN_VER = "gcm";

    private GcmDeviceTokenUtils() {}

    public static void refreshDeviceRegTokens() {
        String gcmSenderId = NuggetInjector.getInstance()
                .getString(com.nuggetchat.messenger.R.string.gcm_defaultSenderId);
        Context appContext = NuggetInjector.getInstance().getAppContext();
        InstanceID instanceID = InstanceID.getInstance(appContext);
        Log.i(LOG_TAG, "Refresh device token senderId " + gcmSenderId);
        try {
            String token = instanceID.getToken(gcmSenderId,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(LOG_TAG, "Got GCM Registration Token: " + token);
            GcmDeviceTokenUtils.saveAllDeviceRegistrationToken(token, appContext);
            NuggetInjector.getInstance().getMixpanel().setupPushHandling(token);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to complete token refresh", e);
        }
    }


    private static void saveAllDeviceRegistrationToken(String deviceRegistrationToken, Context context) {
        String firebaseUid = SharedPreferenceUtility.getFirebaseUid(context);
        String facebookUid = SharedPreferenceUtility.getFacebookUserId(context);

        Log.i(LOG_TAG, "Saving device registration to server");
        saveDeviceRegistrationToken("devices", firebaseUid, deviceRegistrationToken);
        saveDeviceRegistrationToken("devices-facebook", facebookUid, deviceRegistrationToken);
        if (context != null) {
            SharedPreferenceUtility.setFirebaseIdToken(deviceRegistrationToken, context);
            MyLog.i(LOG_TAG, "Saved firebase device token in shared pref ");
        }
    }

    private static void saveDeviceRegistrationToken(String handle, String uid, String deviceRegistrationToken) {
        String userDeviceIDUrl;
        if (FIREBASE_DEVICE_TOKEN.equals(handle)) {
            userDeviceIDUrl = firebaseDeviceToken(uid);
        } else if (FACEBOOK_DEVICE_TOKEN.equals(handle)) {
            userDeviceIDUrl = facebookDeviceToken(uid);
        } else {
            return;
        }
        if (Utils.isNullOrEmpty(userDeviceIDUrl)) {
            MyLog.w(LOG_TAG, "user device id not set");
            return;
        }
        MyLog.i(LOG_TAG, "Storing user's device id at: " + userDeviceIDUrl);
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(userDeviceIDUrl);
        if (firebaseRef == null) {
            return;
        }

        CloudMesgToken cloudMesgToken =
                new CloudMesgToken(CLOUD_MSG_TOKEN_VER, deviceRegistrationToken);

        firebaseRef
                .setValue(cloudMesgToken)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            MyLog.i(LOG_TAG, "Firebase Device Id stored successfully!");
                        } else {
                            Exception exception = task.getException();
                            if (exception != null) {
                                MyLog.e(LOG_TAG, "Unable to update friends.", exception);
                            }
                        }
                    }
                });
    }
}
