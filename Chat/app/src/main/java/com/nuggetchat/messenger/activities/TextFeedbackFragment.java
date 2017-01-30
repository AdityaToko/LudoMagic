package com.nuggetchat.messenger.activities;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.model.DataFormat;
import com.nuggetchat.lib.model.UserFeedback;
import com.nuggetchat.messenger.BuildConfig;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.FeedbackConstants;
import com.nuggetchat.messenger.utils.MyLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TextFeedbackFragment extends DialogFragment {
    private static final String LOG_TAG = TextFeedbackFragment.class.getSimpleName();

    @BindView(R.id.feedback_content) /* package-local */ EditText feedbackContent;
    @BindView(R.id.text_feedback_fragment_question)
    /* package-local */ TextView textFeedbackFragmentQuestion;

    private GamesChatActivity gamesChatActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        gamesChatActivity = (GamesChatActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.text_feedback_fragment, container, false);
        ButterKnife.bind(this, view);

        //injector.logEvent(FirebaseAnalyticsConstants.QA_FEEDBACK_VISITED, null /* bundle */);

        return view;
    }

    @OnClick(R.id.text_feedback_send_button)
    /* package-local */ void sendFeedback() {
        String feedbackContentText = feedbackContent.getText().toString().trim();
        if (DataFormat.isNullOrEmpty(feedbackContentText)) {
            Toast.makeText(gamesChatActivity, R.string.toast_message_on_not_entering_any_feedback,
                    Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            MyLog.w(LOG_TAG, "firebaseuser is null");
            Toast.makeText(gamesChatActivity, R.string.toast_error_message_on_getting_feedback,
                    Toast.LENGTH_LONG).show();
            return;
        }
        final String userId = firebaseUser.getUid();
        if (DataFormat.isNullOrEmpty(userId)) {
            MyLog.w(LOG_TAG, "Empty User ID");
            Toast.makeText(gamesChatActivity, R.string.toast_error_message_on_getting_feedback,
                    Toast.LENGTH_LONG).show();
            return;
        } else if (BuildConfig.DEBUG) {
            MyLog.i(LOG_TAG, "Adding user to firebase " + userId);
        }
        final String feedbackQuestion = textFeedbackFragmentQuestion.getText().toString();
        UserFeedback userFeedback = new UserFeedback(feedbackQuestion, feedbackContentText);

        String userResponseUri = Conf.firebaseUserResponseUri(FeedbackConstants.FEEDBACK_TYPE_QA, userId);
        if (DataFormat.isNullOrEmpty(userResponseUri)) {
            MyLog.w(LOG_TAG, "Empty userResponseUri");
            Toast.makeText(gamesChatActivity, R.string.toast_error_message_on_getting_feedback,
                    Toast.LENGTH_LONG).show();
            return;
        } else if (BuildConfig.DEBUG) {
            MyLog.i(LOG_TAG, "firebase userResponseUri " + userResponseUri);
        }
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(userResponseUri).push();
        if (databaseReference == null) {
            MyLog.w(LOG_TAG, "databaseReference is null");
            Toast.makeText(gamesChatActivity, R.string.toast_error_message_on_getting_feedback,
                    Toast.LENGTH_LONG).show();
            return;
        }
        databaseReference.setValue(userFeedback, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError,
                    DatabaseReference databaseReference) {
                if (databaseError != null) {
                    MyLog.e(LOG_TAG, "Error in saving."
                                    + (BuildConfig.DEBUG ? "context: feedback" : ""),
                            databaseError.toException());
                } else {
                    MyLog.i(LOG_TAG, "Saved successfully."
                            + (BuildConfig.DEBUG ? "context: feedback" : ""));
                }
            }
        });

        feedbackContent.setText(null);
        dismiss();
    }
}
