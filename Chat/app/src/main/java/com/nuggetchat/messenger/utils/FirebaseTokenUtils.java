package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import static com.nuggetchat.lib.Conf.firebaseDomainUri;

public class FirebaseTokenUtils {
    public static final String LOG_TAG = FirebaseTokenUtils.class.getSimpleName();
    private FirebaseTokenUtils() {}

    public static void saveAllDeviceRegistrationToken(String firebaseId, String facebookId, Context context) {
        String deviceRegistrationToken = FirebaseInstanceId.getInstance().getToken();
        saveDeviceRegistrationToken("devices", firebaseId, deviceRegistrationToken);
        saveDeviceRegistrationToken("devices-facebook", facebookId, deviceRegistrationToken);
        if (context != null) {
            SharedPreferenceUtility.setFirebaseIdToken(deviceRegistrationToken, context);
        }
    }

    private static void saveDeviceRegistrationToken(String handle, String uid, String deviceRegistrationToken) {
        String userDeviceIDUrl = firebaseDomainUri() + handle + "/" + uid + "/";
        Log.i(LOG_TAG, "Storing user's device id at: " + userDeviceIDUrl);
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(userDeviceIDUrl);
        if (firebaseRef == null) {
            return;
        }

        firebaseRef
                .setValue(deviceRegistrationToken)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i(LOG_TAG, "Firebase Device Id stored successfully!");
                        } else {
                            Exception exception = task.getException();
                            if (exception != null) {
                                Log.e(LOG_TAG, "Unable to update friends." + exception);
                            }
                        }
                    }
                });
    }
}
