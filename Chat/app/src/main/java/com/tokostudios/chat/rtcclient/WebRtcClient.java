package com.tokostudios.chat.rtcclient;

import android.opengl.EGLContext;
import android.util.Log;

import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.tokostudios.chat.Friend;
import com.tokostudios.chat.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WebRtcClient{
    private static final String LOG_TAG = WebRtcClient.class.getCanonicalName();
    private VideoSource videoSource;
    private PeerConnectionParameters params;
    private User currentUser;
    private boolean initiator = false;
    public List<Peer> peers = new ArrayList<>();
    public List<IceCandidate> queuedRemoteCandidates = new LinkedList<>();
    public List<PeerConnection.IceServer> iceServers = new LinkedList<>();
    public String userId1;
    public String userId2;
    /* package-local */ PeerConnectionFactory factory;
    /* package-local */ MediaConstraints constraints = new MediaConstraints();
    /* package-local */ MediaStream localMediaStream;
    /* package-local */ RtcListener rtcListener;

    public void endCall() {
        setInitiator(false);
        for (Peer peer : peers) {
            peer.getPeerConnection().dispose();
        }
        videoSource.dispose();
        factory.dispose();
        rtcListener.onRemoveRemoteStream();
    }

    public Peer addPeer(User user, Friend friend, Socket socket) {
        Peer peer = new Peer(this, user, friend);
        peer.setLocalStream();
        peer.setSocket(socket);
        peers.add(peer);
        return peer;
    }

    public WebRtcClient(RtcListener listener, PeerConnectionParameters params,
                        EGLContext mEGLcontext, User user1, String iceServerUrls) {
        rtcListener = listener;
        this.params = params;
        PeerConnectionFactory.initializeAndroidGlobals(listener, true /* initializedAudio */,
                true /* initializedVideo */, params.videoCodecHwAcceleration, mEGLcontext);
        factory = new PeerConnectionFactory();
        currentUser = user1;
        userId1 = user1.getId();
        Log.e(LOG_TAG, "User ID 1 is : " + userId1);
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        addIceServers(iceServerUrls);
        setCamera();
    }

    public void addIceServers(String iceServersUrl){
        Log.e(LOG_TAG, "Adding Ice Service Urls: " + iceServersUrl);
        if(iceServersUrl != null || !iceServersUrl.equals("")){
            String[] iceServersArray = iceServersUrl.split(",");
            for(String server : iceServersArray){
                iceServers.add(new PeerConnection.IceServer(server));
            }
        }
    }
    public void addFriendForChat(String userId, Socket socket) {
        User user2 = new User(userId, WebRtcClient.getRandomString());
        Friend friend = new Friend(currentUser, user2, WebRtcClient.getRandomString());
        userId1 = currentUser.getId();
        userId2 = userId;
        addPeer(currentUser, friend, socket);
    }

    public void onPause() {
        if (videoSource != null) videoSource.stop();
    }

    public void onResume() {
        if (videoSource != null) videoSource.restart();
    }

    public void onDestroy() {
        for (Peer peer : peers) {
            peer.getPeerConnection().dispose();
        }
        videoSource.dispose();
        factory.dispose();
    }

    private void setCamera() {
        Log.i(LOG_TAG, "setCamera method");
        localMediaStream = factory.createLocalMediaStream("ARDAMS");
        if (params.videoCallEnabled) {
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(params.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(params.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(params.videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(params.videoFps)));

            videoSource = factory.createVideoSource(getVideoCapturer(), videoConstraints);
            localMediaStream.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource));
        }

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localMediaStream.addTrack(factory.createAudioTrack("ARDAMSa0", audioSource));

        rtcListener.onLocalStream(localMediaStream);
    }

    private VideoCapturer getVideoCapturer() {
        String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }

    public boolean isInitiator() {
        return initiator;
    }

    public void setInitiator(boolean initiator) {
        this.initiator = initiator;
    }

    public void createOffer(Peer peer) {
        peer.getPeerConnection().createOffer(peer, constraints);
    }

    public static String getRandomString() {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }
}
