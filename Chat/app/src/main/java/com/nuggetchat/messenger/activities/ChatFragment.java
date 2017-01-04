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
import android.support.v7.app.AlertDialog;
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
import com.nuggetchat.messenger.chat.User;
import com.nuggetchat.messenger.datamodel.GamesData;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.rtcclient.Peer;
import com.nuggetchat.messenger.rtcclient.PeerConnectionParameters;
import com.nuggetchat.messenger.rtcclient.RtcListener;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
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
//    private VideoRenderer.Callbacks localRender;
//    private VideoRenderer.Callbacks remoteRender;
//    private GLSurfaceView rtcView;
//    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private EglBase eglBase;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private WebRtcClient webRtcClient;
    private User user1;
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
    private int audioManagerMode = AudioManager.MODE_NORMAL;

    private boolean isBound;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            chatService = ((ChatService.ChatBinder)iBinder).getService();
            chatService.registerEventListener(ChatFragment.this);
            if(bundle != null && bundle.getBundle("sdpBundle") != null){
                setSDP();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mainHandler = new Handler(Looper.getMainLooper());
        // Enabled once connected.
        // ViewUtils.setWindowImmersive(getActivity().getWindow());
        view = inflater.inflate(R.layout.activity_chat, container, false);
        ButterKnife.bind(this, view);
        if (SharedPreferenceUtility.getFavFriend1(getActivity()).equals("")) {
            popularFriend1.setVisibility(View.INVISIBLE);
        }

        if (SharedPreferenceUtility.getFavFriend2(getActivity()).equals("")) {
            popularFriend2.setVisibility(View.INVISIBLE);
        }

        bundle = getArguments();
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


        localRenderLayout = (PercentFrameLayout) view.findViewById(R.id.local_layout);
        remoteRenderLayout = (PercentFrameLayout) view.findViewById(R.id.remote_layout);
        localRender = (SurfaceViewRenderer) view.findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) view.findViewById(R.id.remote_video_view);

        String userId = SharedPreferenceUtility.getFacebookUserId(getActivity());
        String username = SharedPreferenceUtility.getFacebookUserName(getActivity());
        Log.e(LOG_TAG, "User is : " + userId + " " + username);
        user1 = new User(userId, username);
        Log.e(LOG_TAG, "User is : " + userId + " " + username);
        /*rtcView = (GLSurfaceView) view.findViewById(R.id.glview_call);
        rtcView.setPreserveEGLContextOnPause(true);
        rtcView.setKeepScreenOn(true);*/
        triggerImageChanges();

        user1 = new User(userId, username);

        /*VideoRendererGui.setView(rtcView, new Runnable() {
            @Override
            public void run() {
                getActivity().bindService(new Intent(getActivity(), ChatService.class), serviceConnection,
                        Context.BIND_AUTO_CREATE);
                init(user1);
                isBound = true;
            }
        });*/
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManagerMode = audioManager.getMode();
       /* remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, false);

        localRender = VideoRendererGui.create(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT, scalingType,
                false);*/
        eglBase = EglBase.create();
        localRender.init(eglBase.getEglBaseContext(), null);
        remoteRender.init(eglBase.getEglBaseContext(), null);

        localRender.setZOrderMediaOverlay(true);
        //        FIXME commented since compile error
//        localRender.setEnableHardwareScaler(true);
//        remoteRender.setEnableHardwareScaler(true);
        updateVideoViews();

        init(user1, targetId);
        getActivity().startService(new Intent(getActivity(), ChatService.class));
        getActivity().bindService(new Intent(getActivity(), ChatService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        isBound = true;

        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEndCallBtn();
                Intent intent = new Intent(ChatFragment.this.getActivity(), FriendsManagerActivity.class);
                intent.putExtra("user_id", "dummy");
                startActivityForResult(intent, 1234);
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject payload = new JSONObject();
                try {
                    Log.e(LOG_TAG, "Users: " + webRtcClient.userId1 + " " + webRtcClient.userId2);
                    payload.put("from", webRtcClient.userId1);
                    payload.put("to", webRtcClient.userId2);
                    payload.put("token", "abcd");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                chatService.socket.emit("end_call", payload);
                webRtcClient.endCall();
                showFriendsAddCluster();
//                VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
//                        LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
                updateVideoViews();
                showStartCallBtn();
            }
        });

        return view;
    }

    private void updateVideoViews() {
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRender.setScalingType(scalingType);
        remoteRender.setMirror(true);
        //FIXME: set Condition for showing remote screen
        if (application.isOngoingCall()) {
            localRenderLayout.setPosition(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT);
            localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        } else {
            localRenderLayout.setPosition(LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                    LOCAL_HEIGHT_CONNECTING);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(true);
        localRender.requestLayout();
        remoteRender.requestLayout();
    }

    @OnClick(R.id.add_friends_to_chat)
    /* package-local */ void addFriendsForCall() {
        Intent intent = new Intent(this.getActivity(), FriendsManagerActivity.class);
        intent.putExtra("user_id", "dummy");
        startActivityForResult(intent, 1234);
    }

    @OnClick({R.id.popular_friend_1})
    /* package-local */ void callFavFriend1() {
        startFriendCall(SharedPreferenceUtility.getFavFriend1(getActivity()));
    }

    @OnClick({R.id.popular_friend_2})
    /* package-local */ void callFavFriend2() {
        startFriendCall(SharedPreferenceUtility.getFavFriend2(getActivity()));
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
        Bundle sdpBundle = bundle.getBundle("sdpBundle");
        if (sdpBundle == null) {
            return;
        }
        Log.d(LOG_TAG, "setSDP " + sdpBundle.toString());
        String type = sdpBundle.getString("type");
        String sdp = sdpBundle.getString("sdp");
        String from = sdpBundle.getString("from");
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), sdp
        );
        webRtcClient.addFriendForChat(from, chatService.socket);
        Peer peer = webRtcClient.peers.get(0);
        peer.getPeerConnection().setRemoteDescription(peer, sessionDescription);
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
                    String thisGameUrl = multiPlayerGamesUrl.get(i)
                            + "?room=" + webRtcClient.userId1
                            + "&user=" + webRtcClient.userId1;
                    String peerGameUrl = multiPlayerGamesUrl.get(i)
                            + "?room=" + webRtcClient.userId1
                            + "&user=" + webRtcClient.userId2;

                    // launch the WebView
                    Intent gameIntent = new Intent(getActivity(), GameWebViewActivity.class);
                    gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_URL, thisGameUrl);
                    startActivity(gameIntent);

                    // emit to peer
                    JSONObject payload = new JSONObject();
                    try {
                        Log.e(LOG_TAG, "Users: " + webRtcClient.userId1 + " " + webRtcClient.userId2);
                        payload.put("from", webRtcClient.userId1);
                        payload.put("to", webRtcClient.userId2);
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

    private void init(User user1) {
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true
        );
        String iceServersString = SharedPreferenceUtility.getIceServersUrls(getActivity());
        webRtcClient = new WebRtcClient(this, params,
                eglBase.getEglBaseContext(), user1, iceServersString,
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
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
//        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
//                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        updateVideoViews();
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        Log.e(LOG_TAG, "inside onAddRemoteStream");
        ViewUtils.setWindowImmersive(getActivity().getWindow(), mainHandler);
        if (remoteStream.videoTracks.size() == 1) {
            application.setOngoingCall(true);
            remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
//            VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
//                    scalingType, true);
//            VideoRendererGui.update(localRender, LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT,
//                    scalingType, true);
            updateVideoViews();
            // TODO: figure out how to do this right and remove the suppression.
            @SuppressWarnings("deprecation")
            boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
            audioManager.setMode(isWiredHeadsetOn ?
                    AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

            showEndCallBtn();
        }
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream) {
        ViewUtils.showWindowNavigation(getActivity().getWindow(), mainHandler);
        application.setOngoingCall(false);
        if (remoteStream != null && remoteStream.videoTracks.size() == 1) {
            remoteStream.videoTracks.get(0).dispose();
        }
//        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
//                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        updateVideoViews();
        resetAudioManager();
    }

    private void resetAudioManager() {
        if (audioManager != null) {
            audioManager.setMode(audioManagerMode);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
       // rtcView.onResume();
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
        super.onPause();
        //rtcView.onPause();
        if (webRtcClient != null) {
            webRtcClient.onPause();
        }

        resetAudioManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webRtcClient != null) {
            JSONObject payload = new JSONObject();
            try {
                Log.e(LOG_TAG, "Users: " + webRtcClient.userId1 + " " + webRtcClient.userId2);
                payload.put("from", webRtcClient.userId1);
                payload.put("to", webRtcClient.userId2);
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
            webRtcClient.endCall();
            undbindService();
        }
    }

    private void startFriendCall(String facebookId) {
        webRtcClient.setInitiator(true);
        application.setInitiator(true);
        webRtcClient.addFriendForChat(facebookId, chatService.socket);
        webRtcClient.createOffer(webRtcClient.peers.get(0));
        SharedPreferenceUtility.setFavouriteFriend(getActivity(), facebookId);
        triggerImageChanges();
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
        showEndCallBtn();
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
                Toast.makeText(getActivity(), data.getStringExtra("user_id"), Toast.LENGTH_LONG).show();
                showEndCallBtn();
                startFriendCall(data.getStringExtra("user_id"));
            }
        }
    }

    @Override
    public void onCall(String userId, Socket socket) {
        if (!webRtcClient.isInitiator()) {
            webRtcClient.addFriendForChat(userId, socket);
        }
        showEndCallBtn();
    }

    @Override
    public void onCallRequestOrAnswer(SessionDescription sdp) {
        Peer peer = webRtcClient.peers.get(0);
        peer.getPeerConnection().setRemoteDescription(peer, sdp);
        showEndCallBtn();
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
        Peer peer = webRtcClient.peers.get(0);
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
                hideFriendsAddCluster();
                startCallButton.setVisibility(View.INVISIBLE);
            }
        });
    }
}
