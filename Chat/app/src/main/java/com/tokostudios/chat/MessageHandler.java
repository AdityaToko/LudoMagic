package com.tokostudios.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.nuggetchat.messenger.NuggetApplication;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.rtcclient.EventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MessageHandler {
    private static final String LOG_TAG = MessageHandler.class.getSimpleName();
    private Socket socket;
    private Context context;
    private EventListener eventListener;
    private List<EventListener> listeners = new ArrayList<>();
    private NuggetApplication application;
    String userId;
    String username;

    public MessageHandler(Socket socket, Context context) {
        this.socket = socket;
        Log.e(LOG_TAG, "MessageHandler: " + socket + " " + "context " + context.getPackageCodePath());
        this.context = context;
        application = (NuggetApplication) context.getApplicationContext();
        userId = SharedPreferenceUtility.getFacebookUserId(context);
        username = SharedPreferenceUtility.getFacebookUserName(context);
        if(username == null || username.equals("")){
            username = WebRtcClient.getRandomString();
        }
        Log.e(LOG_TAG, "MessageHandler: " + userId + " " + username);
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void addEventListener(EventListener eventListener) {
        listeners.add(eventListener);
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
            final JSONObject requestObject = (JSONObject) args[0];
            Log.e(LOG_TAG, "call requested" + args[0].toString());

            if (!application.isInitiator()) {
                Intent intent = new Intent();
                try {
                    String from = requestObject.getString("from");
                    String to = requestObject.getString("to");
                    JSONObject offerObj = requestObject.getJSONObject("offer");
                    String type = offerObj.getString("type");
                    String sdp = offerObj.getString("sdp");
                    Bundle bundle = new Bundle();
                    bundle.putString("from", from);
                    bundle.putString("to", to);
                    bundle.putString("type", type);
                    bundle.putString("sdp", sdp);
                    intent.putExtras(bundle);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.setAction("com.nuggetchat.messenger.intent.action.INCOMING_CALL");
                context.sendBroadcast(intent);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Toast.makeText(context,
                                    "Receiving call from " + requestObject.getString("from"),
                                    Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "JSON ERROR " + e.getMessage());
                        }
                    }
                });
            } else {
                try {
                    // eventListener.onCall(requestObject.getString("from"), socket);
                    for (EventListener listener : listeners) {
                        listener.onCall(requestObject.getString("from"), socket);
                    }
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
                        for (EventListener listener : listeners) {
                            listener.onCallRequestOrAnswer(sdp);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                for (EventListener listener : listeners) {
                    listener.onCall(acceptObject.getString("from"), socket);
                }
                if (userId.equals(acceptObject.getString("to"))
                        && acceptObject.get("answer") != null) {
                    JSONObject answerObj = acceptObject.getJSONObject("answer");
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(answerObj.getString("type")),
                            answerObj.getString("sdp")
                    );
                    for (EventListener listener : listeners) {
                        listener.onCallRequestOrAnswer(sdp);
                    }
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
                for (EventListener listener : listeners) {
                    listener.onCall(iceCandidateObj.getString("from"), socket);
                }
                if (userId.equals(iceCandidateObj.getString("to"))
                        && iceCandidateObj.get("candidate") != null) {
                    Log.e(LOG_TAG, "inside onIceCandidates if ");
                    IceCandidate candidate = new IceCandidate(iceCandidateObj.getString("id"),
                            iceCandidateObj.getInt("label"),
                            iceCandidateObj.getString("candidate"));
                    for (EventListener listener : listeners) {
                        listener.onFetchIceCandidates(candidate);
                    }
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
            for (EventListener listener : listeners) {
                listener.onCallEnd();
            }
            eventListener.onCallEnd();
        }
    };

    public Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    };
}
