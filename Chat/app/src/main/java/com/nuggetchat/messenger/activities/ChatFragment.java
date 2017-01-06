package com.nuggetchat.messenger.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.messenger.NuggetApplication;
import com.nuggetchat.messenger.PercentFrameLayout;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.chat.ChatService;
import com.nuggetchat.messenger.datamodel.GamesData;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.rtcclient.Peer;
import com.nuggetchat.messenger.rtcclient.PeerConnectionParameters;
import com.nuggetchat.messenger.rtcclient.RtcListener;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
import com.nuggetchat.messenger.utils.FirebaseAnalyticsConstants;
import com.nuggetchat.messenger.utils.GlideUtils;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;

public class ChatFragment extends Fragment implements RtcListener, EventListener {
    private static final String LOG_TAG = ChatFragment.class.getSimpleName();
    private static final int LOCAL_X = 3;
    private static final int LOCAL_Y = 3;
    private static final int LOCAL_WIDTH = 25;
    private static final int LOCAL_HEIGHT = 25;
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    Bundle bundle;
    ArrayList<FriendInfo> selectUsers = new ArrayList<>();
    UserFriendsAdapter adapter;
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private EglBase eglBase;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private WebRtcClient webRtcClient;
    private View view;
    private ArrayList<String> multiPlayerGamesName;
    private ArrayList<String> multiPlayerGamesImage;
    private ArrayList<String> multiPlayerGamesUrl;
    private ArrayList<GamesItem> gamesItemList;
    ArrayList<String> gamesName;
    ArrayList<String> gamesImage;
    private NuggetApplication application;
    private ChatService chatService;
    private Handler mainHandler;
    private AudioManager audioManager;
    private int defaultAudioManagerMode = AudioManager.MODE_NORMAL;
    private AudioPlayer audioPlayer;

    private boolean isBound;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(LOG_TAG, "On Service connected");
            chatService = ((ChatService.ChatBinder)iBinder).getService();
            chatService.registerEventListener(ChatFragment.this);
            if(bundle != null && bundle.getBundle("requestBundle") != null){
                acknowledgePreCallHandshake();
            }
        }

        private void acknowledgePreCallHandshake() {
            Log.e(LOG_TAG, "received pre call handshake, sending acknowledgement");
            Bundle requestBundle = bundle.getBundle("requestBundle");
            if (requestBundle == null) {
                return;
            }

            JSONObject requestData = new JSONObject();
            try {
                targetUserId = requestBundle.getString("from");
                requestData.put("from", requestBundle.get("from"));
                requestData.put("to", requestBundle.get("to"));
                requestData.put("token", requestBundle.get("token"));

                onPreCallHandshake(requestData);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            chatService = null;
        }
    };

    @BindView(R.id.friends_add_cluster) LinearLayout linearLayout;
    @BindView(R.id.popular_friend_1) ImageView popularFriend1;
    @BindView(R.id.popular_friend_2) ImageView popularFriend2;
    @BindView(R.id.multipayer_games_view)
    RelativeLayout multiplayerGamesView;
    @BindView(R.id.start_call_button) /* package-local */ ImageView startCallButton;
    @BindView(R.id.end_call_button) /* package-local */ ImageView endCall;
    private VideoRenderer remoteVideoRender;
    private String myUserId;
    private String targetUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mainHandler = new Handler(Looper.getMainLooper());
        //ViewUtils.setWindowImmersive(getActivity().getWindow());
        view = inflater.inflate(R.layout.activity_chat, container, false);
        ButterKnife.bind(this, view);
        if ("".equals(SharedPreferenceUtility.getFavFriend1(getActivity()))) {
            popularFriend1.setVisibility(View.INVISIBLE);
        }

        if ("".equals(SharedPreferenceUtility.getFavFriend2(getActivity()))) {
            popularFriend2.setVisibility(View.INVISIBLE);
        }
        audioPlayer = new AudioPlayer(getActivity());

        bundle = getArguments();
        application = NuggetApplication.getInstance();
        multiPlayerGamesName = new ArrayList<>();
        multiPlayerGamesImage = new ArrayList<>();
        multiPlayerGamesUrl = new ArrayList<>();
        gamesName = new ArrayList<>();
        gamesImage = new ArrayList<>();
        gamesItemList = new ArrayList<>();
        application = (NuggetApplication) getActivity().getApplicationContext();
        fetchData();

        linearLayout.setVisibility(View.VISIBLE);
        getUserFriends();

        initVideoViews();

        myUserId = SharedPreferenceUtility.getFacebookUserId(getActivity());
        Log.e(LOG_TAG, "User is : " + myUserId);
        triggerImageChanges();
        audioManagerInit();
        localRender.setZOrderMediaOverlay(true);
        Log.i(LOG_TAG, "onCreate - call update View");
        updateVideoViews();

        initWebRtc(myUserId);
        bindChatService();
        return view;
    }

    private void audioManagerInit() {
        Log.i(LOG_TAG, "Audio manager Init");
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        defaultAudioManagerMode = audioManager.getMode();
    }

    private void resetAudioManager() {
        Log.i(LOG_TAG, "Audio manager Reset");
        if (audioManager != null) {
            audioManager.setMode(defaultAudioManagerMode);
            audioManager.setSpeakerphoneOn(false);
        }
    }


    private void setLoudSpeakerOn() {
        Log.i(LOG_TAG, "Audio manager set loudspeaker on");
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    private void bindChatService() {
        Log.i(LOG_TAG, " Binding service ");
        getActivity().bindService(new Intent(getActivity(), ChatService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    @OnClick(R.id.start_call_button)
    public void onStartCallBtnClick() {
        showEndCallBtn();
        application.logEvent(getContext(), FirebaseAnalyticsConstants.START_CALL_BUTTON_CLICKED,
                null /* bundle */);
        Intent intent = new Intent(ChatFragment.this.getActivity(), FriendsManagerActivity.class);
        intent.putExtra("user_id", "dummy");
        startActivityForResult(intent, 1234);
    }

    @OnClick(R.id.end_call_button)
    public void onEndCallBtnClick() {
        Log.i(LOG_TAG, "end call Button clicked");
        JSONObject payload = new JSONObject();
        application.logEvent(getContext(),FirebaseAnalyticsConstants.END_CALL_BUTTON_CLICKED,
                null /* bundle */);
        try {
            Log.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
            payload.put("from", myUserId);
            payload.put("to", targetUserId);
            payload.put("token", "abcd");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        chatService.socket.emit("end_call", payload);
        webRtcClient.endCall();
        showFriendsAddCluster();
        updateVideoViews();
        showStartCallBtn();
        audioPlayer.stopRingtone();
    }

    private void initVideoViews() {
        localRenderLayout = (PercentFrameLayout) view.findViewById(R.id.local_layout);
        remoteRenderLayout = (PercentFrameLayout) view.findViewById(R.id.remote_layout);
        eglBase = EglBase.create();
        localRender = (SurfaceViewRenderer) view.findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) view.findViewById(R.id.remote_video_view);
        localRender.init(eglBase.getEglBaseContext(), null);
        remoteRender.init(eglBase.getEglBaseContext(), null);
    }

    private void updateVideoViews() {
        Log.i(LOG_TAG, "Post to Updating video Views");
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (application.isOngoingCall()) {
                    Log.i(LOG_TAG, "On Going call Updating video Views");
                    remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
                    remoteRender.setScalingType(scalingType);
                    remoteRender.setMirror(true);
                    localRenderLayout.setPosition(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT);
                    localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                } else {
                    Log.i(LOG_TAG, "NO call Updating video Views");
                    localRenderLayout.setPosition(LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                            LOCAL_HEIGHT_CONNECTING);
                    localRender.setScalingType(scalingType);
                }
                localRender.setMirror(true);
                localRender.requestLayout();
                remoteRender.requestLayout();
            }
        });
    }

    private void destroyVideoViews() {
//        if (localRender != null) {
//            localRender.release();
//        }
//        if (remoteRender != null) {
//            remoteRender.release();
//        }
//        if (eglBase != null) {
//            eglBase.release();
//        }
    }

    @OnClick(R.id.add_friends_to_chat)
    /* package-local */ void addFriendsForCall() {
        Intent intent = new Intent(this.getActivity(), FriendsManagerActivity.class);
        intent.putExtra("user_id", "dummy");
        application.logEvent(getContext(), FirebaseAnalyticsConstants.ADD_FRIENDS_TO_CHAT_BUTTON_CLICKED,
                null /* bundle */);
        startActivityForResult(intent, 1234);
    }

    @OnClick({R.id.popular_friend_1})
    /* package-local */ void callFavFriend1() {
        application.logEvent(getContext(), FirebaseAnalyticsConstants.POPULAR_FRIEND_1_BUTTON_CLICKED,
                null /* bundle */);
        sendPreCallHandshake(SharedPreferenceUtility.getFavFriend1(getActivity()));
    }

    @OnClick({R.id.popular_friend_2})
    /* package-local */ void callFavFriend2() {
        application.logEvent(getContext(), FirebaseAnalyticsConstants.POPULAR_FRIEND_2_BUTTON_CLICKED,
                null /* bundle */);
        sendPreCallHandshake(SharedPreferenceUtility.getFavFriend2(getActivity()));
    }

    private void undbindService(){
        if(isBound){
            if(chatService != null){
                chatService.unregisterEventListener(this);
            }
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void fetchData() {
        String firebaseUri = Conf.firebaseGamesURI();
        Log.i(LOG_TAG, "Fetching Games Stream : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GamesData gamesDate = dataSnapshot.getValue(GamesData.class);
                gamesName.add(gamesDate.getTitle());
                gamesImage.add(gamesDate.getFeaturedImage());
                GamesItem gamesItem = new GamesItem(dataSnapshot.getKey(), gamesDate.getTitle(),
                        gamesDate.getFeaturedImage(), gamesDate.getUrl(), gamesDate.getPortrait());
                gamesItemList.add(gamesItem);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        String firebaseMultiPlayerGamesUri = Conf.firebaseMultiPlayerGamesUri();
        Log.i(LOG_TAG, "Fetching MultiPlayer Games Stream : , " + firebaseMultiPlayerGamesUri);

        firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseMultiPlayerGamesUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                for (int i = 0 ; i < gamesItemList.size(); i++) {
                    Log.i(LOG_TAG, "games key " + gamesItemList.get(i).getGameKey());
                    if (dataSnapshot.getKey().equals(gamesItemList.get(i).getGameKey())) {
                        Log.i(LOG_TAG, "dataSnapshot games key " + dataSnapshot.getKey());
                        Log.i(LOG_TAG, "games name, " + gamesItemList.get(i).getGamesName());
                        Log.i(LOG_TAG, "games Image, " + gamesItemList.get(i).getGamesImage());
                        multiPlayerGamesName.add(gamesItemList.get(i).getGamesName());
                        multiPlayerGamesImage.add(gamesItemList.get(i).getGamesImage());
                        multiPlayerGamesUrl.add(gamesItemList.get(i).getGamesUrl());
                        Log.i(LOG_TAG, "the size , " + multiPlayerGamesName.size());
                    }
                }

                for (int i = 0 ; i < multiPlayerGamesName.size(); i++) {
                    setUpListView(i);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setSDP(){
        Log.i(LOG_TAG, "calling setSDP");
        Bundle sdpBundle = bundle.getBundle("requestBundle");
        if (sdpBundle == null) {
            return;
        }
        Log.d(LOG_TAG, "setSDP " + sdpBundle.toString());
        String type = sdpBundle.getString("type");
        String sdp = sdpBundle.getString("sdp");
        targetUserId = sdpBundle.getString("from");
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), sdp
        );

        Peer peer = webRtcClient.getPeer();
        if (peer != null) {
            peer.getPeerConnection().setRemoteDescription(peer, sessionDescription);
        }
    }

    private void setUpListView(final int i) {
        Log.i(LOG_TAG, "multiplayer game  " + i);

        LinearLayout gamesList = (LinearLayout) view.findViewById(R.id.games_list);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.grid_item, gamesList, false);
        TextView textView = (TextView) view.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView) view.findViewById(R.id.grid_image);
        Log.i(LOG_TAG, "multiplayer game name, " + multiPlayerGamesName.get(i));
        Log.i(LOG_TAG, "multiplayer game image, " + multiPlayerGamesImage.get(i));

        textView.setText(multiPlayerGamesName.get(i));
        String imageURl = Conf.CLOUDINARY_PREFIX_URL + multiPlayerGamesImage.get(i);
        Log.d("The image uri " , imageURl);
        GlideUtils.loadImage(getActivity(), imageView, null, imageURl);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.isOngoingCall()) {
                    application.logEvent(getContext(), FirebaseAnalyticsConstants.MULTIPLAYER_GAMES_BUTTON_CLICKED,
                            null /* bundle */);
                    String thisGameUrl = multiPlayerGamesUrl.get(i)
                            + "?room=" + myUserId
                            + "&user=" + myUserId;
                    String peerGameUrl = multiPlayerGamesUrl.get(i)
                            + "?room=" + myUserId
                            + "&user=" + targetUserId;

                    // launch the WebView
                    Intent gameIntent = new Intent(getActivity(), GameWebViewActivity.class);
                    gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_URL, thisGameUrl);
                    startActivity(gameIntent);

                    // emit to peer
                    JSONObject payload = new JSONObject();
                    try {
                        Log.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
                        payload.put("from", myUserId);
                        payload.put("to", targetUserId);
                        payload.put("token", "abcd");
                        payload.put("game_link", peerGameUrl);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    chatService.socket.emit("game_link", payload);
                } else {
                    Toast.makeText(getActivity(), "Please select a friend to start playing game with!", Toast.LENGTH_LONG).show();
                }
            }
        });

        gamesList.addView(view);
    }

    private void initWebRtc(String myUserId) {
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true
        );
        String iceServersString = SharedPreferenceUtility.getIceServersUrls(getActivity());
        webRtcClient = new WebRtcClient(this, params,
                eglBase.getEglBaseContext(), myUserId, iceServersString,
                getActivity());
    }

    @Override
    public void onCallReady(String callId) {

    }

    @Override
    public void onStatusChanged(String newStatus) {
        Log.i(LOG_TAG, "On Status Changed: " + newStatus);
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.i(LOG_TAG, "onLocalStream");
        if (!localStream.videoTracks.isEmpty()) {
            localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
            updateVideoViews();
        } else {
            Log.w(LOG_TAG, "Video tracks empty");
        }

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        Log.e(LOG_TAG, "inside onAddRemoteStream");
        ViewUtils.setWindowImmersive(getActivity().getWindow(), mainHandler);
        if (!remoteStream.videoTracks.isEmpty()) {
            application.setOngoingCall(true);
            remoteVideoRender = new VideoRenderer(remoteRender);
            remoteStream.videoTracks.get(0).addRenderer(remoteVideoRender);
            updateVideoViews();
            setLoudSpeakerOn();
            hideFriendsAddCluster();
        } else {
            Log.w(LOG_TAG, "Remote video tracks empty");
        }
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream) {
        Log.i(LOG_TAG, "on Remove Remote stream");
        ViewUtils.showWindowNavigation(getActivity().getWindow(), mainHandler);
        application.setOngoingCall(false);
        if (remoteStream != null && !remoteStream.videoTracks.isEmpty()) {
            if (remoteVideoRender != null) {
                remoteStream.videoTracks.get(0).removeRenderer(remoteVideoRender);
                remoteVideoRender = null;
            }
            updateVideoViews();
            resetAudioManager();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webRtcClient != null) {
            webRtcClient.onResume();
        }
        if (application.isOngoingCall() || application.isInitiator()) {
            showEndCallBtn();
        }
        if (bundle != null) {
            Log.d(LOG_TAG, "bundle not null " + bundle.getString("user_id"));
            if (bundle.getString("user_id") == null) {
                Log.d(LOG_TAG, "START CALL OnRESUME");
            }
        } else {
            Log.d(LOG_TAG, "bundle null");
        }
    }

    @Override
    public void onPause() {
        //rtcView.onPause();
        if (webRtcClient != null) {
            webRtcClient.onPause();
        }
        resetAudioManager();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        destroyVideoViews();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (webRtcClient != null) {
            if (application.isOngoingCall()) {
                JSONObject payload = new JSONObject();
                try {
                    Log.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
                    payload.put("from", myUserId);
                    payload.put("to", targetUserId);
                    payload.put("token", "abcd");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (chatService != null && chatService.socket != null) {
                    chatService.socket.emit("end_call", payload);
                } else {
                    String errStr = "Chat service or socket null";
                    Log.e(LOG_TAG, errStr);
                    throw new IllegalStateException(errStr);
                }
            }
            webRtcClient.endCall();
            undbindService();
        }
        audioPlayer.stopRingtone();
        super.onDestroy();
    }

    private void sendPreCallHandshake(String facebookId) {
        webRtcClient.setInitiator(true);
        application.setInitiator(true);
        targetUserId = facebookId;
        webRtcClient.addFriendForChat(facebookId, chatService.socket);

        JSONObject payload = new JSONObject();
        try {
            payload.put("from", myUserId);
            payload.put("to", targetUserId);
            payload.put("token", "abcd");
            chatService.socket.emit("pre_call_handshake", payload);
            Log.e(LOG_TAG, "pre call handshake sent.." + payload.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        hideFriendsAddCluster();
        SharedPreferenceUtility.setFavouriteFriend(getActivity(), facebookId);
        triggerImageChanges();
        audioPlayer.playRingtone();
        endCall.setVisibility(View.VISIBLE);
        startCallButton.setVisibility(View.INVISIBLE);
    }

    private void startFriendCall(String facebookId) {
        Log.i(LOG_TAG, "start friend call");
        webRtcClient.setInitiator(true);
        application.setInitiator(true);
        webRtcClient.addFriendForChat(facebookId, chatService.socket);
        audioPlayer.playRingtone();
        Peer peer = webRtcClient.getPeer();
        if (peer != null) {
            webRtcClient.createOffer(peer);
            SharedPreferenceUtility.setFavouriteFriend(getActivity(), facebookId);
            triggerImageChanges();
        }
        endCall.setVisibility(View.VISIBLE);
        startCallButton.setVisibility(View.INVISIBLE);
    }

    private void triggerImageChanges() {
        String friend1 = SharedPreferenceUtility.getFavFriend1(getActivity());
        String friend2 = SharedPreferenceUtility.getFavFriend2(getActivity());
        if (!friend1.equals("")) {
            String friendOnePicUrl = "https://graph.facebook.com/" + friend1 + "/picture?width=200&height=150";
            Glide.with(getActivity()).load(friendOnePicUrl).asBitmap().centerCrop().into(new BitmapImageViewTarget(popularFriend1) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(getActivity().getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    popularFriend1.setImageDrawable(circularBitmapDrawable);
                }
            });
            popularFriend1.setVisibility(View.VISIBLE);
        }
        if (!friend2.equals("")) {
            String friendTwoPicUrl = "https://graph.facebook.com/" + friend2 + "/picture?width=200&height=150";
            Glide.with(getActivity()).load(friendTwoPicUrl).asBitmap().centerCrop().into(new BitmapImageViewTarget(popularFriend2) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(getActivity().getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    popularFriend2.setImageDrawable(circularBitmapDrawable);
                }
            });
            popularFriend2.setVisibility(View.VISIBLE);
        }
    }

    private void hideFriendsAddCluster() {
        multiplayerGamesView.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.INVISIBLE);
    }

    private void showFriendsAddCluster() {
        endCall.setVisibility(View.INVISIBLE);
        multiplayerGamesView.setVisibility(View.INVISIBLE);
        Log.d(LOG_TAG, "START CALL SHOW FRIENDS CLUSTER");
        startCallButton.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
    }

    private void getUserFriends() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.d(LOG_TAG, response.toString());
                        JSONObject object = response.getJSONObject();
                        try {
                            for (int i = 0; i < object.getJSONArray("data").length(); i++) {
                                JSONObject dataObject = (object.getJSONArray("data")).getJSONObject(i);
                                Log.d(LOG_TAG, dataObject.toString());
                                String name = dataObject.getString("name");
                                String userId = dataObject.getString("id");
                                FriendInfo userData = new FriendInfo();
                                userData.setFacebookId(userId);
                                userData.setName(name);
                                selectUsers.add(userData);
                                Log.d(LOG_TAG, "Values " + name + "  " + userId);
                            }
                            adapter = new UserFriendsAdapter(selectUsers, getActivity());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(LOG_TAG, "fragment onActivityResult");
        if (requestCode == 1234 && data != null) {
            Log.d(LOG_TAG, "before toast onActivityResult");
            if (data != null) {
                showEndCallBtn();
                sendPreCallHandshake(data.getStringExtra("user_id"));
            }
        }
    }

    @Override
    public void onPreCallHandshake(JSONObject data) {
        if (!webRtcClient.isInitiator()) {
            try {
                targetUserId = data.getString("from");
                String from = data.getString("from");
                String to = data.getString("to");
                String token = data.getString("token");
                if (myUserId.equals(to) && "abcd".equals(token)) {
                    webRtcClient.addFriendForChat(from, chatService.socket);

                    JSONObject payload = new JSONObject();
                    payload.put("from", myUserId);
                    payload.put("to", targetUserId);
                    payload.put("token", token);
                    chatService.socket.emit("handshake_complete", payload);
                    showEndCallBtn();
                    Log.e(LOG_TAG, "pre call handshake complete.. sending handshake_complete");
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public void onHandshakeComplete(JSONObject data) {
        if (webRtcClient.isInitiator()) {
            try {
                String from = data.getString("from");
                String to = data.getString("to");
                String token = data.getString("token");

                if (myUserId.equals(to)
                        && targetUserId.equals(from)
                        && "abcd".equals(token)) {
                    webRtcClient.createOffer(webRtcClient.getPeer());
                    Log.e(LOG_TAG, "when handshake complete... create offer");
                }
            } catch(JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public void onCall(String userId, Socket socket) {
        // if (!webRtcClient.isInitiator()) {
        //     webRtcClient.addFriendForChat(userId, socket);
        // }
        // showEndCallBtn();
    }

    @Override
    public void onCallRequestOrAnswer(SessionDescription sdp) {
        Peer peer = webRtcClient.getPeer();
        if (peer != null) {
            peer.getPeerConnection().setRemoteDescription(peer, sdp);
            showEndCallBtn();
        }
        audioPlayer.stopRingtone();
    }

    @Override
    public void onGameLink(String link) {
        // launch the WebView
        Intent gameIntent = new Intent(getActivity(), GameWebViewActivity.class);
        gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_URL, link);
        startActivity(gameIntent);
    }

    @Override
    public void onCallEnd() {
        webRtcClient.endCall();
        showStartCallBtn();
    }

    @Override
    public void onFetchIceCandidates(IceCandidate candidate) {
        Peer peer = webRtcClient.getPeer();
        if ( peer == null) {
            return;
        }
        if (webRtcClient.queuedRemoteCandidates != null) {
            if (!webRtcClient.queuedRemoteCandidates.isEmpty()) {
                Log.e(LOG_TAG, "local desc before queueing peers :" +
                        peer.getPeerConnection().getLocalDescription());
                Log.e(LOG_TAG, "remote desc before queueing peers :" +
                        peer.getPeerConnection().getRemoteDescription());
                webRtcClient.queuedRemoteCandidates.add(candidate);
            }

        } else {
            Log.e(LOG_TAG, "local desc before adding peers :" +
                    peer.getPeerConnection().getLocalDescription());
            Log.e(LOG_TAG, "remote desc before adding peers :" +
                    peer.getPeerConnection().getRemoteDescription());
            peer.getPeerConnection().addIceCandidate(candidate);
        }
    }

    private void showStartCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endCall.setVisibility(View.INVISIBLE);
                startCallButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showEndCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endCall.setVisibility(View.VISIBLE);
                startCallButton.setVisibility(View.INVISIBLE);
            }
        });
    }
}
