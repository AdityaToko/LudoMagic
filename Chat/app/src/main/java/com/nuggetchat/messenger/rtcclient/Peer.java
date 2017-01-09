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
        Log.i(LOG_TAG, "Peer created ");
        this.webRtcClient = webRtcClient;
        peerConnection = webRtcClient.factory.createPeerConnection(webRtcClient.iceServers,
                webRtcClient.constraints, this);
        webRtcClient.rtcListener.onStatusChanged("CONNECTING");
    }

    public void setSocket(Socket socket) {
        Log.i(LOG_TAG, "setSocket");
        this.socket = socket;
    }

    public void setLocalStream() {
        Log.i(LOG_TAG, "setLocalStream");
        peerConnection.addStream(webRtcClient.localMediaStream);
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void resetPeerConnection() {
        Log.i(LOG_TAG, "resetPeerConnection & dispose");
        if (peerConnection != null) {
            Log.i(LOG_TAG, "MessageHandler reset Peer connection disposing");
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
        Log.i(LOG_TAG, "onIceCandidate called");
        JSONObject payload = new JSONObject();
        try {
            payload.put("label", iceCandidate.sdpMLineIndex);
            payload.put("id", iceCandidate.sdpMid);
            payload.put("candidate", iceCandidate.sdp);
            payload.put("from", webRtcClient.getUserId1());
            payload.put("to", webRtcClient.getUserId2());
            payload.put("token", "abcd");
            Log.i(LOG_TAG, "MessageHandler emit ice_candidates");
            socket.emit("ice_candidates", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.i(LOG_TAG, "onAddStream: " + mediaStream.label());
        webRtcClient.rtcListener.onAddRemoteStream(mediaStream);

        Log.i(LOG_TAG, "local: " + (peerConnection.getLocalDescription() != null));
        Log.i(LOG_TAG, "remote: " + (peerConnection.getRemoteDescription() != null));
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.i(LOG_TAG, "onRemoveStream: & Closing " + mediaStream.label());
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
        Log.i(LOG_TAG, "onCreateSuccess called");

        SessionDescription sdp = new SessionDescription(sessionDescription.type, sessionDescription.description);
        localSdp = sdp;
        JSONObject payload = new JSONObject();
        try {
            payload.put("type", sdp.type.canonicalForm());
            payload.put("sdp", sdp.description);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, payload.toString() + "");
        peerConnection.setLocalDescription(this, sdp);
    }

    @Override
    public void onSetSuccess() {
        Log.i(LOG_TAG, "onSetSuccess called");
        new Runnable() {
            @Override
            public void run() {
                if (webRtcClient.isInitiator() ) {
                    if (peerConnection.getRemoteDescription() != null) {
                        Log.i(LOG_TAG, "initiator + remote desc done");
                        drainRemoteCandidates();
                    } else {
                        Log.i(LOG_TAG, "Offer generated sending offer");
                        sendOfferLocalDescription();
                    }
                } else {
                    if (peerConnection.getLocalDescription() == null) {
                        Log.i(LOG_TAG, "local desc not set. Create answer");
                        peerConnection.createAnswer(Peer.this, webRtcClient.constraints);
                    } else {
                        Log.i(LOG_TAG, "Answer generated sending answer");
                        sendAnswerLocalDescription();
                        drainRemoteCandidates();
                    }
                }

            }
        }.run();
    }

    private void sendOfferLocalDescription() {
        Log.i(LOG_TAG, "sendOfferLocalDescription: sending Offer");
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
            Log.i(LOG_TAG, "MessageHandler emit request_call");
            socket.emit("request_call", callData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendAnswerLocalDescription() {
        Log.i(LOG_TAG, "sendAnswerLocalDescription: sending answer");
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
            Log.i(LOG_TAG, "MessageHandler emit accept_call " + callData.toString() );
            socket.emit("accept_call", callData);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onCreateFailure(String s) {
        Log.i(LOG_TAG, "Failed to create something in peer" + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.i(LOG_TAG, "Failed to set something in peer" + s);
    }

    private void drainRemoteCandidates() {
        Log.i(LOG_TAG, "Drain remote candidate");
        if (webRtcClient.queuedRemoteCandidates == null) {
            return;
        }
        for (IceCandidate candidate : webRtcClient.queuedRemoteCandidates) {
            Log.i(LOG_TAG, "Drain remote candidate " + candidate + " set succesful");
            peerConnection.addIceCandidate(candidate);
        }
        webRtcClient.queuedRemoteCandidates = null;
    }
}
