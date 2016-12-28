package com.tokostudios.chat;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.tokostudios.chat.rtcclient.EventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MessageHandler {
    private static final String LOG_TAG = MessageHandler.class.getSimpleName();
    private Socket socket;
    private Context context;
    private EventListener eventListener;
    String userId;
    String username;

    public MessageHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
        userId = SharedPreferenceUtility.getFacebookUserId(context);
        username = SharedPreferenceUtility.getFacebookUserName(context);
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public Emitter.Listener onInit = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(LOG_TAG, "inside onInit");
            JSONObject user = new JSONObject();
            try {
                user.put("userId", userId);
                user.put("username", username);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("init", user);
            socket.emit("message", user.toString());
        }
    };

    public Emitter.Listener onInitSuccessful = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(LOG_TAG, "inside onInitSuccessful");
            socket.emit("message", "Init successful");
            try {
                JSONObject successObj = (JSONObject) args[0];
                JSONArray stunUrls = successObj.getJSONObject("iceServers").
                        getJSONObject("stun").getJSONArray("urls");
                StringBuilder stunUrlsBuilder = new StringBuilder();
                for (int i = 0; i < stunUrls.length(); i++) {
                    stunUrlsBuilder.append(stunUrls.getString(i));
                    stunUrlsBuilder.append(",");
                }
                Log.e(LOG_TAG, stunUrlsBuilder.toString());
                SharedPreferenceUtility.setIceServersUrls(stunUrlsBuilder.toString(), context);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onCallRequested = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Toast.makeText(context, "Call Requested", Toast.LENGTH_SHORT);
            JSONObject requestObject = (JSONObject) args[0];
            Log.e(LOG_TAG, "call requested" + args[0].toString());
            try {
                eventListener.onCall(requestObject.getString("from"), socket);
                Log.e(LOG_TAG, "call requested inside try" + " " + requestObject.getString("to"));
                if (userId.equals(requestObject.getString("to"))
                        && requestObject.getJSONObject("offer") != null) {
                    Log.e(LOG_TAG, "call requested inside if");
                    JSONObject offerObj = requestObject.getJSONObject("offer");
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(offerObj.getString("type")),
                            offerObj.getString("sdp")
                    );
                    Log.e(LOG_TAG, "Setting remote desc after onCallRequested for " + requestObject.getString("to"));
                    eventListener.onCallRequestOrAnswer(sdp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onCallAccepted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG, "inside onCallAccepted ");
            JSONObject acceptObject = (JSONObject) args[0];
            try {
                eventListener.onCall(acceptObject.getString("from"), socket);
                if (userId.equals(acceptObject.getString("to"))
                        && acceptObject.get("answer") != null) {
                    JSONObject answerObj = acceptObject.getJSONObject("answer");
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(answerObj.getString("type")),
                            answerObj.getString("sdp")
                    );
                    eventListener.onCallRequestOrAnswer(sdp);
                    Log.e(LOG_TAG, "Setting remote desc after onCallAccepted for " + acceptObject.getString("to"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onIceCandidates = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(LOG_TAG, "inside onIceCandidates ");

            try {
                JSONObject iceCandidateObj = (JSONObject) args[0];
                eventListener.onCall(iceCandidateObj.getString("from"), socket);
                if (userId.equals(iceCandidateObj.getString("to"))
                        && iceCandidateObj.get("candidate") != null) {
                    Log.e(LOG_TAG, "inside onIceCandidates if ");
                    IceCandidate candidate = new IceCandidate(iceCandidateObj.getString("id"),
                            iceCandidateObj.getInt("label"),
                            iceCandidateObj.getString("candidate"));
                    eventListener.onFetchIceCandidates(candidate);
                    Log.e(LOG_TAG, "setting ice candidates successfully for :" + iceCandidateObj.getString("to"));
                } else {
                    Log.e(LOG_TAG, "candidate is null or " + userId
                            + " is  not equal to :" + iceCandidateObj.getString("to"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    public Emitter.Listener onCallEnded = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG, "inside onCallEnded");
            eventListener.onCallEnd();
        }
    };
}
