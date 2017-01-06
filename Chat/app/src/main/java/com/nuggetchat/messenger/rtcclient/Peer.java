package com.nuggetchat.messenger.rtcclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;

public class Peer implements PeerConnection.Observer, SdpObserver {
    private static final String LOG_TAG = Peer.class.getSimpleName();
    private PeerConnection peerConnection;
    private WebRtcClient webRtcClient;
    private SessionDescription localSdp;
    private Socket socket;

    public Peer(WebRtcClient webRtcClient) {
        Log.d(LOG_TAG, "Peer created ");
        this.webRtcClient = webRtcClient;
        peerConnection = webRtcClient.factory.createPeerConnection(webRtcClient.iceServers,
                webRtcClient.constraints, this);
        webRtcClient.rtcListener.onStatusChanged("CONNECTING");
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setLocalStream() {
        Log.e(LOG_TAG, "setLocalStream called ");
        peerConnection.addStream(webRtcClient.localMediaStream);
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void resetPeerConnection() {
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            //TODO remove peer id
            webRtcClient.rtcListener.onStatusChanged("DISCONNECTED");
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.e(LOG_TAG, "onIceCandidate called");
        JSONObject payload = new JSONObject();
        try {
            payload.put("label", iceCandidate.sdpMLineIndex);
            payload.put("id", iceCandidate.sdpMid);
            payload.put("candidate", iceCandidate.sdp);
            payload.put("from", webRtcClient.getUserId1());
            payload.put("to", webRtcClient.getUserId2());
            payload.put("token", "abcd");
            socket.emit("ice_candidates", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(LOG_TAG, "onAddStream: " + mediaStream.label());
        webRtcClient.rtcListener.onAddRemoteStream(mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(LOG_TAG, "onRemoveStream: " + mediaStream.label());
        webRtcClient.rtcListener.onRemoveRemoteStream(mediaStream);
        peerConnection.close();
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(LOG_TAG, "onCreateSuccess called");

        SessionDescription sdp = new SessionDescription(sessionDescription.type, sessionDescription.description);
        localSdp = sdp;
        JSONObject payload = new JSONObject();
        try {
            payload.put("type", sdp.type.canonicalForm());
            payload.put("sdp", sdp.description);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, payload.toString() + "");
        peerConnection.setLocalDescription(this, sdp);
    }

    @Override
    public void onSetSuccess() {
        Log.d(LOG_TAG, "onSetSuccess called");
        new Runnable() {
            @Override
            public void run() {
                if (webRtcClient.isInitiator() ) {
                    if (peerConnection.getRemoteDescription() != null) {
                        if (webRtcClient.queuedRemoteCandidates != null) {
                            Log.e(LOG_TAG, "remote desc is set. Draining candidates. Count : "
                                    + webRtcClient.queuedRemoteCandidates.size());
                            drainRemoteCandidates();
                        }
                    } else {
                        sendOfferLocalDescription();
                    }
                } else {
                    if (peerConnection.getLocalDescription() == null) {
                        Log.e(LOG_TAG, "local desc not set. Create answer");
                        peerConnection.createAnswer(Peer.this, webRtcClient.constraints);
                    } else {
                        sendAnswerLocalDescription();
                        if (webRtcClient.queuedRemoteCandidates != null) {
                            Log.e(LOG_TAG, "Sending answer desc. and draining candidates. Count " +
                                    webRtcClient.queuedRemoteCandidates.size());
                            drainRemoteCandidates();
                        }

                    }
                }

            }
        }.run();
    }

    private void sendOfferLocalDescription() {
        Log.e(LOG_TAG, "sendOfferLocalDescription: sending Offer");
        JSONObject callData = new JSONObject();
        JSONObject localDesc = new JSONObject();
        Log.e(LOG_TAG, "sendOfferLocalDescription: " + webRtcClient.getUserId1() + " " + webRtcClient.getUserId2());
        try {
            localDesc.put("type", localSdp.type.canonicalForm());
            localDesc.put("sdp", localSdp.description);
            callData.put("from", webRtcClient.getUserId1());
            callData.put("to", webRtcClient.getUserId2());
            callData.put("offer", localDesc);
            callData.put("token", "abcd");
            socket.emit("request_call", callData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendAnswerLocalDescription() {
        Log.e(LOG_TAG, "sendAnswerLocalDescription: sending answer");
        JSONObject callData = new JSONObject();
        JSONObject localDesc = new JSONObject();
        try {
            Log.e(LOG_TAG, "sendAnswerLocalDescription: inside try");
            localDesc.put("type", localSdp.type.canonicalForm());
            localDesc.put("sdp", localSdp.description);
            callData.put("from", webRtcClient.getUserId1());
            callData.put("to", webRtcClient.getUserId2());
            callData.put("token", "abcd");
            callData.put("answer", localDesc);
            Log.e(LOG_TAG, "sendAnswerLocalDescription: " + callData.toString() );
            socket.emit("accept_call", callData);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onCreateFailure(String s) {

    }

    @Override
    public void onSetFailure(String s) {

    }

    private void drainRemoteCandidates() {
        for (IceCandidate candidate : webRtcClient.queuedRemoteCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        webRtcClient.queuedRemoteCandidates = null;
    }
}
