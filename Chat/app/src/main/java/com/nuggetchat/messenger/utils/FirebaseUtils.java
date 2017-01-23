package com.nuggetchat.messenger.utils;

import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.lib.model.DataFormat;
import com.nuggetchat.messenger.datamodel.GamePlayedHistory;

public class FirebaseUtils {
    public static final String LOG_TAG = FirebaseUtils.class.getSimpleName();

    public static void writeCallMade (String from, String to, String gameID) {
        Log.d(LOG_TAG, ">>> From uid" + from);
        Log.d(LOG_TAG, ">>> To uid" + to);
        Log.d(LOG_TAG, ">>> gameID" + gameID);

        long playedTS = System.currentTimeMillis()/1000;

        GamePlayedHistory gamePlayedHistoryFrom = new GamePlayedHistory(gameID, playedTS, true, to);
        GamePlayedHistory gamePlayedHistoryTo = new GamePlayedHistory(gameID, playedTS, true, from);

        writeCallMadeToFirebase(from, gamePlayedHistoryFrom);
        writeCallMadeToFirebase(to, gamePlayedHistoryTo);
    }

    public static void writeCallMadeToFirebase(String userId, GamePlayedHistory gamePlayedHistory) {
        String fbaseUserHistoryUri;
        if (DataFormat.isNotNullNorEmpty(userId)) {
            String currentDate = Utils.getCurrentDate();
            if(gamePlayedHistory.isMultiplayer()) {
                fbaseUserHistoryUri  = Conf.firebaseMultiGamePlayedURI(userId, currentDate);
            } else {
                fbaseUserHistoryUri  = Conf.firebaseSoloGamePlayedURI(userId, currentDate);
            }
            Log.d(LOG_TAG,">>>Writing to: " + fbaseUserHistoryUri);

            DatabaseReference histRef =
                    FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseUserHistoryUri);
            fireBaseSetValue(histRef.push(), gamePlayedHistory, "history user-id:" + userId);
        } else {

        }
    }

    public static <T> void fireBaseSetValue(DatabaseReference fbaseRef, T value,
                                            final String userContext) {
        if (fbaseRef != null) {
            fbaseRef.setValue(value, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Log.e(LOG_TAG, "Error in saving."
                                        + databaseError.toException());
                    } else {
                        Log.i(LOG_TAG, "Saved successfully."
                                + "context: " + userContext);
                    }
                }
            });
        }
    }

}
