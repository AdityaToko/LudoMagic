package com.nuggetchat.messenger.chat;

import android.content.Context;

import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
import com.nuggetchat.messenger.utils.FirebaseUtils;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

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
    private NuggetInjector nuggetInjector;
    private String userId;
    private String username;
    private UpdateInterface updatesListener;

    public MessageHandler(Socket socket, Context context) {
        this.socket = socket;
        MyLog.e(LOG_TAG, "MessageHandler: " + socket + " " + "context " + context.getPackageCodePath());
        this.context = context;
        nuggetInjector = NuggetInjector.getInstance();
        userId = SharedPreferenceUtility.getFacebookUserId(context);
        username = SharedPreferenceUtility.getFacebookUserName(context);
        if("".equals(username)){
            username = WebRtcClient.getRandomString();
        }
        MyLog.e(LOG_TAG, "MessageHandler: " + userId + " " + username);
    }

    public void addEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }
    public void registerUpdatesListener(UpdateInterface updatesListener){
        this.updatesListener = updatesListener;
    }
    public Emitter.Listener onInit = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onInit");
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
            MyLog.i(LOG_TAG, "onInitSuccessful");
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
                MyLog.i(LOG_TAG, stunUrlsBuilder.toString());
                SharedPreferenceUtility.setIceServersUrls(stunUrlsBuilder.toString(), context);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onPreCallHandshake = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onPreCallHandshake");
            eventListener.onPreCallHandshake((JSONObject) args[0]);
        }
    };

    public Emitter.Listener onHandshakeComplete = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onHandshakeComplete");
            eventListener.onHandshakeComplete((JSONObject) args[0]);
        }
    };

    public Emitter.Listener onCallRequested = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject requestObject = (JSONObject) args[0];
            MyLog.i(LOG_TAG, "onCallRequested" + args[0].toString());

            try {
                if (requestObject.getJSONObject("offer") != null) {
                    MyLog.i(LOG_TAG, "onCallRequested if");
                    JSONObject offerObj = requestObject.getJSONObject("offer");
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(offerObj.getString("type")),
                            offerObj.getString("sdp")
                    );
                    MyLog.i(LOG_TAG, "onCallRequested to: " + requestObject.getString("to"));
                    eventListener.onCallRequestOrAnswer(sdp);
                } else {
                    MyLog.w(LOG_TAG, "onCallRequested offer null");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onCallAccepted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onCallAccepted");
            if (nuggetInjector.isOngoingCall()) {
                MyLog.w(LOG_TAG, "onCallAccepted Ignore other incoming calls");
                return;
            }
            JSONObject acceptObject = (JSONObject) args[0];
            try {
                if (acceptObject.get("answer") != null) {
                    JSONObject answerObj = acceptObject.getJSONObject("answer");

                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(answerObj.getString("type")),
                            answerObj.getString("sdp")
                    );
                    eventListener.onCallRequestOrAnswer(sdp);
                    MyLog.i(LOG_TAG, "onCallAccepted to:" + acceptObject.getString("to"));
                } else {
                    MyLog.w(LOG_TAG, "onCallAccepted answer null");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onCallRejected = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onCallRejected");

            eventListener.onCallRejected();
        }
    };

    public Emitter.Listener onSocketError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onSocketError");
        }
    };

    public Emitter.Listener onIceCandidates = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onIceCandidates");

            try {
                JSONObject iceCandidateObj = (JSONObject) args[0];
                if (iceCandidateObj.get("candidate") != null) {
                    MyLog.i(LOG_TAG, "onIceCandidates if ");
                    IceCandidate candidate = new IceCandidate(iceCandidateObj.getString("id"),
                            iceCandidateObj.getInt("label"),
                            iceCandidateObj.getString("candidate"));
                    eventListener.onFetchIceCandidates(candidate);
                    MyLog.i(LOG_TAG, "onIceCandidates to:" + iceCandidateObj.getString("to"));
                } else {
                    MyLog.e(LOG_TAG, "onIceCandidates null" );
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onGameLink = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
        MyLog.i(LOG_TAG, "onGameLink");
        JSONObject gameLinkObject = (JSONObject) args[0];
        try {
            String from = gameLinkObject.getString("from");
            String to = gameLinkObject.getString("to");
            String gameID = gameLinkObject.getString("gameID");
            FirebaseUtils.writeGamePlayed(from, to, gameID);
            updatesListener.updateReceiverScore(to, from);

            eventListener.onCall(gameLinkObject.getString("from"),socket);
            if (gameLinkObject.getString("game_link") != null) {
                eventListener.onGameLink(gameLinkObject.getString("game_link"));
            } else {
                MyLog.w(LOG_TAG, "onGameLink game_link null");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        }
    };

    public Emitter.Listener onCallEnded = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onCallEnded");
            eventListener.onCallEnd();
        }
    };

    public Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onDisconnect");
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    };

    public Emitter.Listener onCallOngoing = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MyLog.i(LOG_TAG, "onDisconnect");
            eventListener.onCallOngoing();
        }
    };
}
