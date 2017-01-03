package com.nuggetchat.messenger.rtcclient;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import com.nuggetchat.messenger.NuggetApplication;
import com.nuggetchat.messenger.chat.Friend;
import com.nuggetchat.messenger.chat.User;

import org.webrtc.AudioSource;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import io.socket.client.Socket;

public class WebRtcClient{
    private static final String LOG_TAG = WebRtcClient.class.getSimpleName();
    private VideoSource videoSource;
    private PeerConnectionParameters params;
    private User currentUser;
    private boolean initiator = false;
    public NuggetApplication application;
    private Context context;
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
        application.setInitiator(false);
        for (Peer peer : peers) {
            peer.resetPeerConnection();
        }

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        rtcListener.onRemoveRemoteStream(null);
    }

    public Peer addPeer(User user, Friend friend, Socket socket) {
        Peer peer = new Peer(this, user, friend);
        peer.setLocalStream();
        peer.setSocket(socket);
        peers.add(peer);
        return peer;
    }

    public WebRtcClient(RtcListener listener, PeerConnectionParameters params,  EglBase.Context mEGLcontext
                        /*EGLContext mEGLcontext*/, User user1, String iceServerUrls, Context context) {
        rtcListener = listener;
        this.params = params;
        PeerConnectionFactory.initializeAndroidGlobals(context, true /* initializedAudio */,
                true /* initializedVideo */, params.videoCodecHwAcceleration/*, mEGLcontext*/);
        factory = new PeerConnectionFactory();
        factory.setVideoHwAccelerationOptions(mEGLcontext, mEGLcontext);
        this.context = context;
        application = (NuggetApplication) context.getApplicationContext();
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
        iceServersUrl = "stun:stun.services.mozilla.com,stun:stun.l.google.com:19302,";
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
        //if (videoSource != null) videoSource.stop();
        if(videoSource!=null){
            videoSource.dispose();
        }
    }

    public void onResume() {
       // if (videoSource != null) videoSource.restart();
        VideoCapturer videoCapturer = getVideoCapturer();
        if(videoCapturer != null){
            videoCapturer.startCapture(params.videoWidth, params.videoHeight, params.videoFps);
        }
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

           // videoSource = factory.createVideoSource(getVideoCapturer(), videoConstraints);
            videoSource = factory.createVideoSource(getVideoCapturer());
            localMediaStream.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource));
        }

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localMediaStream.addTrack(factory.createAudioTrack("ARDAMSa0", audioSource));

        rtcListener.onLocalStream(localMediaStream);
    }

    private VideoCapturer getVideoCapturer() {
        /*String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        return VideoCapturerAndroid.create(frontCameraDeviceName);*/
        return createCameraCapturer(new Camera2Enumerator(context));
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
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
