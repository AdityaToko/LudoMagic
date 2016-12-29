package com.nuggetchat.messenger.rtcclient;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;

public interface EventListener {

    void onCall(String userId, Socket socket);

    void onCallRequestOrAnswer(SessionDescription sdp);

    void onCallEnd();

    void onFetchIceCandidates(IceCandidate candidate);
}
