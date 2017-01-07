package com.nuggetchat.messenger.rtcclient;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;

public interface EventListener {

    void onPreCallHandshake(JSONObject data);

    void onHandshakeComplete(JSONObject data);

    void onCall(String userId, Socket socket);

    void onCallRequestOrAnswer(SessionDescription sdp);

    void onGameLink(String link);

    void onCallEnd();

    void onFetchIceCandidates(IceCandidate candidate);

    void onCallRejected();

    void onCallOngoing();
}
