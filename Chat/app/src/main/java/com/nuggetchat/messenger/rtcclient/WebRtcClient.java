package com.nuggetchat.messenger.rtcclient;

import android.content.Context;
import android.util.Log;

import com.nuggetchat.messenger.NuggetApplication;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import io.socket.client.Socket;

public class WebRtcClient{
    private static final String LOG_TAG = WebRtcClient.class.getSimpleName();
    private VideoSource videoSource;
    private PeerConnectionParameters params;
    private String currentUserId;
    private boolean initiator = false;
    private NuggetApplication application;
    public List<IceCandidate> queuedRemoteCandidates;
    public List<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private String userId1;
    private String userId2;
    private Peer peer;
    /* package-local */ PeerConnectionFactory factory;
    /* package-local */ MediaConstraints constraints = new MediaConstraints();
    /* package-local */ MediaStream localMediaStream;
    /* package-local */ RtcListener rtcListener;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private AudioSource audioSource;

    public WebRtcClient(RtcListener listener, PeerConnectionParameters params, EglBase.Context mEGLcontext
                        /*EGLContext mEGLcontext*/, String currentUserId, String iceServerUrls, Context context) {
        rtcListener = listener;
        this.params = params;
        PeerConnectionFactory.initializeAndroidGlobals(context, true /* initializedAudio */,
                true /* initializedVideo */, params.videoCodecHwAcceleration/*, mEGLcontext*/);
        factory = new PeerConnectionFactory();
        factory.setVideoHwAccelerationOptions(mEGLcontext, mEGLcontext);
        application = (NuggetApplication) context.getApplicationContext();
        this.currentUserId = currentUserId;
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        addIceServers(iceServerUrls);
        setCameraAndUpdateVideoViews();
    }


    public void endCallAndRemoveRemoteStream() {
        Log.i(LOG_TAG, "End call - Incoming");
        setInitiator(false);
        application.setInitiator(false);
        if (peer != null) {

            if (localMediaStream != null) {
                if (videoTrack != null) {
                    localMediaStream.removeTrack(videoTrack);
                    videoTrack = null;
                }
                if (audioTrack != null) {
                    localMediaStream.removeTrack(audioTrack);
                    audioTrack = null;
                }
                if (videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }
                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }
                localMediaStream.dispose();
                localMediaStream = null;
            }
            peer.resetPeerConnection();
            Log.i(LOG_TAG, "peer reset done");
        }
        Log.i(LOG_TAG, "rtc update video view");
        if (rtcListener != null) {
            rtcListener.onRemoveRemoteStream(null); // will also update video views
        }
    }

    public Peer addPeer(Socket socket) {
        queuedRemoteCandidates = new LinkedList<>();
        Peer newPeer = new Peer(this);
        newPeer.setLocalStream();
        newPeer.setSocket(socket);
        return newPeer;
    }

    public void addIceServers(String iceServersUrl){
        Log.e(LOG_TAG, "Adding Ice Service Urls: " + iceServersUrl);
        if(iceServersUrl != null && !"".equals(iceServersUrl)){
            String[] iceServersArray = iceServersUrl.split(",");
            for(String server : iceServersArray){
                iceServers.add(new PeerConnection.IceServer(server));
            }
        }
    }
    public void addFriendForChat(String userId, Socket socket) {
        Log.i(LOG_TAG, "addFriendForChat userId: " + userId);
        userId1 = currentUserId;
        userId2 = userId;
        peer = addPeer(socket);
    }

    public String getUserId1() {
        return userId1;
    }

    public String getUserId2() {
        return userId2;
    }

    public Peer getPeer() {
        return peer;
    }

    public void onPause() {
        if (videoSource != null) {
            videoSource.stop();
        }
    }

    public void onResume() {
        if (videoSource != null) {
            videoSource.restart();
        }
    }

    public void setCameraAndUpdateVideoViews() {
        Log.i(LOG_TAG, "setCameraAndUpdateVideoViews method");
        localMediaStream = factory.createLocalMediaStream("ARDAMS");

        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(params.videoHeight)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(params.videoWidth)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(params.videoFps)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(params.videoFps)));
        if(videoSource == null){
            videoSource = factory.createVideoSource(getVideoCapturer(), videoConstraints);
        }
        Log.i(LOG_TAG, "Adding video track");
        videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        localMediaStream.addTrack(videoTrack);

        if (audioSource == null) {
            audioSource = factory.createAudioSource(new MediaConstraints());
        }
        Log.i(LOG_TAG, "Adding audio track");
        audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        localMediaStream.addTrack(audioTrack);

        Log.i(LOG_TAG, "Trigger local stream");
        rtcListener.onLocalStream(localMediaStream);  // Updating video views
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = { "front", "back" };
//        int[] cameraIndex = { 0, 1 };
        int[] cameraIndex = { 1 };
//        int[] cameraOrientation = { 0, 90, 180, 270 };
        int[] cameraOrientation = { 270 };
        for (String facing : cameraFacing) {
            for (int index : cameraIndex) {
                for (int orientation : cameraOrientation) {
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    VideoCapturer capturer = VideoCapturer.create(name);
                    if (capturer != null) {
                        Log.i(LOG_TAG, "Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new IllegalStateException("Failed to open capturer");
    }

    public void disposePeerConnnectionFactory(){
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
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
