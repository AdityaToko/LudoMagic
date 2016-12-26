package com.tokostudios.chat.webRtcClient;

import android.opengl.EGLContext;
import android.util.Log;

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

public class WebRtcClient {
    private static final String LOG_TAG = WebRtcClient.class.getCanonicalName();
    private VideoSource videoSource;
    private PeerConnectionParameters params;
    private User currentUser;
    public List<Peer> peers = new ArrayList<>();
    private boolean initiator = false;
    LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<>();
    public Socket socket;
    /* package-local */ PeerConnectionFactory factory;
    /* package-local */ LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    /* package-local */ MediaConstraints constraints = new MediaConstraints();
    /* package-local */ MediaStream localMediaStream;
    /* package-local */ RtcListener rtcListener;
    public String userId1;
    public String userId2;

    private class MessageHandler {

        private Emitter.Listener onInit = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject user = new JSONObject();
                try {
                    user.put("userId", currentUser.getId());
                    user.put("username", currentUser.getName());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                socket.emit("init", user);
                socket.emit("message", user.toString());
            }
        };

        private Emitter.Listener onInitSuccessful = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("message", "Init successful");
                try {
                    JSONObject successObj = (JSONObject) args[0];
                    JSONArray stunUrls = successObj.getJSONObject("iceServers").
                            getJSONObject("stun").getJSONArray("urls");
                    for (int i = 0; i < stunUrls.length(); i++) {
                        iceServers.add(new PeerConnection.IceServer(stunUrls.getString(i)));
                    }
                    //start("Aman";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        private Emitter.Listener onCallRequested =  new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                JSONObject argsObject = (JSONObject) args[0];
                Log.e(LOG_TAG, "call requested" + args[0].toString() );
                try {
                    Peer peer = peers.get(0);
                    Log.e(LOG_TAG, "call requested inside try" + " " +peer.getUser().getId() + " " + argsObject.getString("to"));
                    if (userId1.equals(argsObject.getString("to"))
                            && argsObject.getJSONObject("offer") != null) {
                        Log.e(LOG_TAG, "call requested inside if" );
                        JSONObject offerObj = argsObject.getJSONObject("offer");
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(offerObj.getString("type")),
                                offerObj.getString("sdp")
                        );
                        Log.e(LOG_TAG, "Setting remote desc after onCallRequested for " + argsObject.getString("to"));
                        peer.getPeerConnection().setRemoteDescription(peer, sdp);
                        Log.e(LOG_TAG, "getRemoteDescription  " + peer.getPeerConnection().getRemoteDescription().description+
                                " "+ peer.getPeerConnection().getRemoteDescription().type.canonicalForm());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        private Emitter.Listener onCallAccepted = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "inside onCallAccepted ");
                JSONObject argsObject = (JSONObject) args[0];
                try {
                    Peer peer = peers.get(0);
                    if (userId1.equals(argsObject.getString("to"))
                            && argsObject.get("answer") != null) {
                        JSONObject answerObj = argsObject.getJSONObject("answer");
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(answerObj.getString("type")),
                                answerObj.getString("sdp")
                        );
                        Log.e(LOG_TAG, "Setting remote desc after onCallAccepted for "+ argsObject.getString("to"));
                        peer.getPeerConnection().setRemoteDescription(peer, sdp);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        private Emitter.Listener onIceCandidates = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(LOG_TAG, "inside onIceCandidates ");

                try {
                    JSONObject iceCandidateObj = (JSONObject) args[0];
                    Peer peer = peers.get(0);
                    if (userId1.equals(iceCandidateObj.getString("to"))
                            && iceCandidateObj.get("candidate") != null) {
                        Log.e(LOG_TAG, "inside onIceCandidates if ");
                        IceCandidate candidate = new IceCandidate(iceCandidateObj.getString("id"),
                                iceCandidateObj.getInt("label"),
                                iceCandidateObj.getString("candidate"));
                        if (queuedRemoteCandidates != null) {
                            Log.e(LOG_TAG, "local desc before queueing peers :" +
                                    peer.getPeerConnection().getLocalDescription());
                            Log.e(LOG_TAG, "remote desc before queueing peers :" +
                                    peer.getPeerConnection().getRemoteDescription());
                            queuedRemoteCandidates.add(candidate);
                        } else {
                            Log.e(LOG_TAG, "local desc before adding peers :" +
                                    peer.getPeerConnection().getLocalDescription());
                            Log.e(LOG_TAG, "remote desc before adding peers :" +
                                    peer.getPeerConnection().getRemoteDescription());
                            peer.getPeerConnection().addIceCandidate(candidate);
                        }
                        Log.e(LOG_TAG, "setting ice candidates successfully for :" + iceCandidateObj.getString("to"));
                    } else {
                        Log.e(LOG_TAG, "candidate is null or " + userId1
                                + " is  not equal to :" + iceCandidateObj.getString("to") );
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };


        private Emitter.Listener onCallEnded = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "inside onCallEnded");
                //peers.get(0).getPeerConnection().close();
                videoSource.dispose();
                factory.dispose();
               // rtcListener.onRemoveRemoteStream();
            }
        };
    }
    public void endCall(){
        setInitiator(false);
        videoSource.dispose();
        factory.dispose();
        //rtcListener.onRemoveRemoteStream();
        //peers.get(0).getPeerConnection().close();
    }

    public Peer addPeer(User user, Friend friend) {
        Peer peer = new Peer(this, user, friend);
        peer.setLocalStream();
        peers.add(peer);
        return peer;
    }

    public WebRtcClient(RtcListener listener, String host, PeerConnectionParameters params,
                        EGLContext mEGLcontext, User user1, String targetId) {
        rtcListener = listener;
        this.params = params;
        PeerConnectionFactory.initializeAndroidGlobals(listener, true /* initializedAudio */,
                true /* initializedVideo */, params.videoCodecHwAcceleration, mEGLcontext);
        factory = new PeerConnectionFactory();
        MessageHandler messageHandler = new MessageHandler();
        currentUser = user1;
        User user2 = new User(targetId,WebRtcClient.getRandomString());
        Friend friend = new Friend(user1, user2, WebRtcClient.getRandomString());
        userId1 = user1.getId();
        Log.e(LOG_TAG, "User ID 1 is : " + userId1);
        //userId2 = user2.getId();
        try {
            socket = IO.socket(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "WebRtcClient: inside");
        socket.on(Socket.EVENT_CONNECT, messageHandler.onInit);
        socket.on("init_successful", messageHandler.onInitSuccessful);
        socket.on("call_requested", messageHandler.onCallRequested);
        socket.on("call_accepted", messageHandler.onCallAccepted);
        socket.on("ice_candidates", messageHandler.onIceCandidates);
        socket.on("call_ended", messageHandler.onCallEnded);
        socket.connect();

        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        setCamera();
        addPeer(user1, friend);

    }

    public void startCall(Friend friend) {
        JSONObject participants = new JSONObject();
        try {
            participants.put("userId", friend.getUser1().getId());
            participants.put("friendId", friend.getUser2().getId());
            participants.put("token", friend.getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this method in Activity.onPause()
     */
    public void onPause() {
        if (videoSource != null) videoSource.stop();
    }

    /**
     * Call this method in Activity.onResume()
     */
    public void onResume() {
        if (videoSource != null) videoSource.restart();
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    public void onDestroy() {
        for (Peer peer : peers) {
            peer.getPeerConnection().dispose();
        }
        videoSource.dispose();
        factory.dispose();
        socket.disconnect();
        socket.close();
    }

    /**
     * Start the socket.
     * <p>
     * Set up the local stream and notify the signaling server.
     * Call this method after onCallReady.
     *
     * @param name socket name
     */
    public void start(String name) {
        setCamera();
        try {
            JSONObject message = new JSONObject();
            message.put("name", name);
            socket.emit("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
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

    public void createAnswer(Peer peer) {
        peer.getPeerConnection().createAnswer(peer, constraints);
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
