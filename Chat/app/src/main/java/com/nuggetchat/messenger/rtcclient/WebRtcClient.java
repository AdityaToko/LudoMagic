package com.nuggetchat.messenger.rtcclient;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.nuggetchat.messenger.base.NuggetInjector;
import com.nuggetchat.messenger.utils.MyLog;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import io.socket.client.Socket;

public class WebRtcClient{
    private static final String LOG_TAG = WebRtcClient.class.getSimpleName();
    private VideoSource videoSource;
    private PeerConnectionParameters params;
    private String currentUserId;
    private NuggetInjector nuggetInjector;
    private boolean isQueueDrainedOnce;
    private AtomicBoolean queueLock;
    public List<IceCandidate> queuedRemoteCandidates = null;
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
    private VideoCapturerAndroid videoCapturer;

    public WebRtcClient(RtcListener listener, PeerConnectionParameters params, EglBase.Context mEGLcontext
                        /*EGLContext mEGLcontext*/, String currentUserId, String iceServerUrls, Context context) {
        MyLog.i(LOG_TAG, "Init");
        rtcListener = listener;
        this.params = params;
        isQueueDrainedOnce = false;
        PeerConnectionFactory.initializeAndroidGlobals(context, true /* initializedAudio */,
                true /* initializedVideo */, params.videoCodecHwAcceleration/*, mEGLcontext*/);
        factory = new PeerConnectionFactory();
        factory.setVideoHwAccelerationOptions(mEGLcontext, mEGLcontext);
        nuggetInjector = NuggetInjector.getInstance();
        this.currentUserId = currentUserId;
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        queueLock = new AtomicBoolean(false);
        addIceServers(iceServerUrls);
        setCameraAndUpdateVideoViews();
    }


    public void endCallAndRemoveRemoteStream() {
        MyLog.i(LOG_TAG, "End call");
        nuggetInjector.setInitiator(false);
        if (peer != null) {
            peer.resetPeerConnection();
            MyLog.i(LOG_TAG, "peer reset done");
        }
        if (rtcListener != null) {
            rtcListener.onRemoveRemoteStream(null); // will also update video views
        }
        queuedRemoteCandidates = null;
        isQueueDrainedOnce = false;
        queueLock.set(false);
    }

    public Peer addPeer(Socket socket) {
        MyLog.i(LOG_TAG, "Add peer");
        Peer newPeer = new Peer(this);
        newPeer.setLocalStream();
        newPeer.setSocket(socket);
        queuedRemoteCandidates = new LinkedList<>();
        isQueueDrainedOnce = false;
        queueLock.set(false);
        return newPeer;
    }

    public void addIceServers(String iceServersUrl){
        MyLog.e(LOG_TAG, "Add Ice Service Urls: " + iceServersUrl);
        if(iceServersUrl != null && !"".equals(iceServersUrl)){
            String[] iceServersArray = iceServersUrl.split(",");
            for(String server : iceServersArray){
                iceServers.add(new PeerConnection.IceServer(server));
            }
        }
    }
    public void addFriendForChat(String userId, Socket socket) {
        MyLog.i(LOG_TAG, "addFriendForChat userId: " + userId);
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

    public void onResume() {
        MyLog.i(LOG_TAG, "onResume");
        restartVideoSource();
    }

    public void onPause() {
        MyLog.i(LOG_TAG, "onPause");
        stopVideoSource();
    }

    public void stopVideoSource() {
        if (videoSource != null) {
            Log.e(LOG_TAG, "Stopping video source.......");
            videoSource.stop();
            videoSource.stop();
        }
    }

    public void restartVideoSource() {
        if (videoSource != null) {
            Log.e(LOG_TAG, "Restarting video source.......");
            videoSource.restart();
        }
    }

    public void addVideoSource() {
        if (localMediaStream != null) {
            if (videoTrack != null) {
                localMediaStream.addTrack(videoTrack);
            }
        }
    }

    public void removeVideoSource() {
        if (localMediaStream != null) {
            if (videoTrack != null) {
                localMediaStream.removeTrack(videoTrack);
            }
        }
    }

    public void setCameraAndUpdateVideoViews() {
        MyLog.i(LOG_TAG, "setCameraAndUpdateVideoViews method");
        localMediaStream = factory.createLocalMediaStream("ARDAMS");

        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(params.videoHeight)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(params.videoWidth)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(params.videoFps)));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(params.videoFps)));
        videoCapturer = getNewVideoCapturer();
        if (videoCapturer == null) {
            return;
        }
        if(videoSource == null){
            videoSource = factory.createVideoSource(videoCapturer, videoConstraints);
        }
        MyLog.i(LOG_TAG, "Adding video track");
        videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        localMediaStream.addTrack(videoTrack);

        if (audioSource == null) {
            audioSource = factory.createAudioSource(new MediaConstraints());
        }
        MyLog.i(LOG_TAG, "Adding audio track");
        audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        localMediaStream.addTrack(audioTrack);

        MyLog.i(LOG_TAG, "Trigger local stream");
        rtcListener.onLocalStream(localMediaStream);  // Updating video views
    }

    public void releaseLocalMediaOnDestrory() {
        MyLog.i(LOG_TAG, "Release camera");
        if (rtcListener != null) {
            rtcListener.onRemoveLocalStream(localMediaStream);
        }
        MyLog.i(LOG_TAG, "Release camera 1");

        if (videoTrack != null) {
            videoTrack.dispose();
        }

        MyLog.i(LOG_TAG, "Release camera 2");

        if (videoSource != null) {
            videoSource.stop();
        }

        MyLog.i(LOG_TAG, "Release camera 3");

        if (localMediaStream != null) {
            if (audioTrack != null) {
                localMediaStream.removeTrack(audioTrack);
            }
            if (videoTrack != null) {
                localMediaStream.removeTrack(videoTrack);
            }
            MyLog.i(LOG_TAG, "Release camera 3.1");
            localMediaStream = null;
        }
        MyLog.i(LOG_TAG, "Release camera 4");

        if (videoCapturer != null && !videoCapturer.isReleased()) {
            MyLog.i(LOG_TAG, "Video capturer dispose");
            videoCapturer.dispose();
            videoCapturer = null;
        }
        MyLog.i(LOG_TAG, "Release camera 5");

        if (videoSource != null) {
            MyLog.i(LOG_TAG, "Video source null");
            videoSource = null;
        }
        MyLog.i(LOG_TAG, "Release camera 6");
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturerAndroid getNewVideoCapturer() {
        String name = "Camera " + 1 + ", Facing " + "front" + ", Orientation " + 270;
        VideoCapturerAndroid capturer = VideoCapturerAndroid.create(name,  getNewCameraEventsHandler());
        if (capturer != null) {
            MyLog.i(LOG_TAG, "Using camera: " + name);
            return capturer;
        }
        MyLog.e(LOG_TAG, "Camera not found: " + name);
        return null;
    }

    @NonNull
    private VideoCapturerAndroid.CameraEventsHandler getNewCameraEventsHandler() {
        return new VideoCapturerAndroid.CameraEventsHandler() {
                @Override
                public void onCameraError(String s) {
                    MyLog.e(LOG_TAG, "onCameraError: " + s );
                    if(videoCapturer != null){
                        videoCapturer.printStackTrace();
                    }
                }

                @Override
                public void onCameraFreezed(String s) {
                    MyLog.e(LOG_TAG, "onCameraFreezed: " + s );
                    if(videoCapturer != null){
                        videoCapturer.printStackTrace();
                    }
                }

                @Override
                public void onCameraOpening(int i) {
                    Log.i(LOG_TAG, "onCameraOpening: " + i);
                }

                @Override
                public void onFirstFrameAvailable() {
                    Log.i(LOG_TAG, "onFirstFrameAvailable");
                    rtcListener.onLocalStreamFirstFrame();
                }

                @Override
                public void onCameraClosed() {
                    Log.i(LOG_TAG, "onCameraClosed");
                }
            };
    }

    public void disposePeerConnnectionFactory(){
        MyLog.i(LOG_TAG, "disposePeerConnection");

        if (peer != null) {
            MyLog.i(LOG_TAG, "peer reset connection");
            peer.resetPeerConnection();
        }

        if (factory != null) {
            factory.dispose();
            factory = null;
        }
    }

    public boolean isInitiator() {
        return nuggetInjector.isInitiator();
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

    /*package local*/ void lockAndDrainRemoteCandidates(PeerConnection peerConnection, boolean retry) {
        if (!queueLock.getAndSet(true)) {
            MyLog.i(LOG_TAG, "Got lock for draining. retry:" + retry);
            try {
                drainRemoteCandidates(peerConnection);
            } catch (Exception e) {
                MyLog.e(LOG_TAG, "Draining failed", e);
            } finally {
                queueLock.set(false);
            }
        } else if (!retry) {
            MyLog.i(LOG_TAG, "Retrying for lock draining");
            try {
                Thread.sleep(200);
                lockAndDrainRemoteCandidates(peerConnection, true /*retry*/);
            } catch (InterruptedException e) {
                MyLog.e(LOG_TAG, "Sleep & drain interrupted ", e);
            }
        } else {
            MyLog.e(LOG_TAG, "Retry for lock failed draining");
        }
    }

    private void drainRemoteCandidates(PeerConnection peerConnection) {
        MyLog.i(LOG_TAG, "Drain remote candidate");
        if (queuedRemoteCandidates == null) {
            MyLog.w(LOG_TAG, "Queue null");
            return;
        }
        if (peerConnection == null) {
            MyLog.w(LOG_TAG, "Peer connection null");
            return;
        }
        for (IceCandidate candidate : queuedRemoteCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        queuedRemoteCandidates = null;
        isQueueDrainedOnce = true;
    }

    public boolean lockAndQueueRemoteCandidates(IceCandidate candidate) {
        boolean isQueued = false;
        if (!queueLock.getAndSet(true)){
            MyLog.i(LOG_TAG, "Got lock for queuing");
            isQueued = queueRemoteCandidates(candidate);
            queueLock.set(false);
        }
        MyLog.i(LOG_TAG, "Tried lock for queuing queued:" + isQueued );
        return isQueued;
    }

    private boolean queueRemoteCandidates(IceCandidate candidate) {
        MyLog.i(LOG_TAG, "Adding remote candidate to queue");
        if (!isQueueDrainedOnce && queuedRemoteCandidates != null) {
            queuedRemoteCandidates.add(candidate);
            return true;
        }
        return false;
    }

    public void addIceCandidateToPeerConnection(IceCandidate candidate) {
        if ( peer == null) {
            return;
        }
        PeerConnection peerConnection = peer.getPeerConnection();
        if (peerConnection == null) {
            return;
        }
        peerConnection.addIceCandidate(candidate);
    }
}
