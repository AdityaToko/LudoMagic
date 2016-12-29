package com.nuggetchat.messenger.rtcclient;

import org.webrtc.MediaStream;

public interface RtcListener {
        void onCallReady(String callId);

        void onStatusChanged(String newStatus);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream);

        void onRemoveRemoteStream(MediaStream remoteStream);
    }
