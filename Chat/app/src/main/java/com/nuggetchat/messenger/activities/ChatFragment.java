package com.nuggetchat.messenger.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.model.FriendInfo;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.FragmentChangeListener;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.services.ChatService;
import com.nuggetchat.messenger.chat.IncomingCallActivity;
import com.nuggetchat.messenger.datamodel.GamesData;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.rtcclient.Peer;
import com.nuggetchat.messenger.rtcclient.PeerConnectionParameters;
import com.nuggetchat.messenger.rtcclient.RtcListener;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
import com.nuggetchat.messenger.utils.AnalyticConstants;
import com.nuggetchat.messenger.utils.GlideUtils;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;

public class ChatFragment extends Fragment implements RtcListener, EventListener, FragmentChangeListener {
    public static final int INCOMING_CALL_CODE = 7890;
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
    @BindView(R.id.friends_add_cluster)
    LinearLayout linearLayout;
    @BindView(R.id.popular_friend_1)
    ImageView popularFriend1;
    @BindView(R.id.popular_friend_2)
    ImageView popularFriend2;
    @BindView(R.id.multipayer_games_view)
    RelativeLayout multiplayerGamesView;
    @BindView(R.id.text_play_with_friends)
    TextView textPlayWithFriends;
    @BindView(R.id.end_call_button) /* package-local */ ImageView endCall;
    @BindView(R.id.end_busy_call_button) /* package-local */ ImageView endBusyCallBtn;
    @BindView(R.id.video_disabled) /* package-local */ ImageView videoDisabledButton;

    private GLSurfaceView videoCallView;
    private VideoRenderer.Callbacks local;
    private VideoRenderer.Callbacks remote;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private WebRtcClient webRtcClient;
    private View view;
    private ArrayList<GamesItem> gamesItemList;
    private ArrayList<GamesItem> multiplayerGamesItemList;
    private ArrayList<String> multiplayerIDList;
    private NuggetInjector nuggetInjector;
    private ChatService chatService;
    private Handler mainHandler;
    private AudioManager audioManager;
    private int defaultAudioManagerMode = AudioManager.MODE_NORMAL;
    private GamesChatActivity gamesChatActivity;
    private AudioPlayer audioPlayer;
    private Handler handler;
    private boolean hasAudioFocus;
    private boolean isBound;
    private VideoRenderer remoteVideoRender;
    private VideoRenderer localRenderer;
    private String myUserId;
    private String targetUserId;
    private boolean videoVisible;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyLog.i(LOG_TAG, "On Service connected");
            chatService = ((ChatService.ChatBinder) iBinder).getService();
            chatService.registerEventListener(ChatFragment.this);
            chatService.registerUpdatesListener(gamesChatActivity);
            if (bundle != null && bundle.getBundle("requestBundle") != null) {
                Bundle requestBundle = bundle.getBundle("requestBundle");
                if (requestBundle.getString("user_id") != null) {
                    sendPreCallHandshake(bundle.getString("user_id"));
                }
                if ("pre_call_handshake".equals(requestBundle.getString("type"))) {
                    if (requestBundle.getBoolean(IncomingCallActivity.CALL_ACCEPTED)) {
                        acknowledgePreCallHandshake(bundle);
                    } else {
                        rejectCall(bundle);
                    }
                }
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            chatService = null;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MyLog.i(LOG_TAG, "onCreateView");
        mainHandler = new Handler(Looper.getMainLooper());
        gamesChatActivity = (GamesChatActivity) getActivity();

//        ViewUtils.setWindowImmersive(gamesChatActivity.getWindow());
        view = inflater.inflate(R.layout.activity_chat, container, false);
        ButterKnife.bind(this, view);
        if ("".equals(SharedPreferenceUtility.getFavFriend1(gamesChatActivity))) {
            popularFriend1.setVisibility(View.INVISIBLE);
        }

        if ("".equals(SharedPreferenceUtility.getFavFriend2(gamesChatActivity))) {
            popularFriend2.setVisibility(View.INVISIBLE);
        }
        audioPlayer = AudioPlayer.getInstance(getActivity());

        bundle = getArguments();
        nuggetInjector = NuggetInjector.getInstance();
        gamesItemList = new ArrayList<>();
        multiplayerGamesItemList = new ArrayList<>();
        multiplayerIDList = new ArrayList<>();

        fetchDataForGames(this.getContext());
        handler = new Handler();

        linearLayout.setVisibility(View.VISIBLE);
        getUserFriends();
        videoCallView = (GLSurfaceView) view.findViewById(R.id.video_call_view);
        videoCallView.setPreserveEGLContextOnPause(true);
        videoCallView.setKeepScreenOn(true);
        VideoRendererGui.setView(videoCallView, new Runnable() {
            @Override
            public void run() {
                videoCallView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        });
        remote = VideoRendererGui.create(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, true);
        local = VideoRendererGui.create(LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                LOCAL_HEIGHT_CONNECTING, scalingType, true);

        myUserId = SharedPreferenceUtility.getFacebookUserId(gamesChatActivity);
        MyLog.e(LOG_TAG, "User is : " + myUserId);
        triggerImageChanges();
        audioManagerInit();
        MyLog.i(LOG_TAG, "onCreate - call update View");
        initWebRtc(myUserId);
        bindChatService();
        ViewUtils.hideViewsFromAppsee(videoCallView, LOG_TAG);
        videoVisible = true;
        return view;
    }


    private void audioManagerInit() {
        MyLog.i(LOG_TAG, "Audio manager Init");
        audioManager = (AudioManager) gamesChatActivity.getSystemService(Context.AUDIO_SERVICE);
        defaultAudioManagerMode = audioManager.getMode();
    }

    private void resetAudioManager() {
        MyLog.i(LOG_TAG, "Audio manager Reset");
        if (audioManager != null) {
            audioManager.setMode(defaultAudioManagerMode);
            audioManager.setSpeakerphoneOn(false);
        }
    }


    private void setLoudSpeakerOn() {
        MyLog.i(LOG_TAG, "Audio manager set loudspeaker on");
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    private void bindChatService() {
        MyLog.i(LOG_TAG, " Binding service ");
        gamesChatActivity.bindService(new Intent(gamesChatActivity, ChatService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    @OnClick(R.id.end_call_button)
    public void onEndCallBtnClick() {
        MyLog.i(LOG_TAG, "end call Button clicked");
        audioPlayer.stopRingtone();
        JSONObject payload = new JSONObject();
        nuggetInjector.logEvent(AnalyticConstants.END_CALL_BUTTON_CLICKED,
                null /* bundle */);
        try {
            MyLog.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
            payload.put("from", myUserId);
            payload.put("to", targetUserId);
            payload.put("token", "abcd");
            payload.put("caller", SharedPreferenceUtility.getFacebookUserName(gamesChatActivity));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MyLog.i(LOG_TAG, "MessageHandler emit end_call");
        chatService.socket.emit("end_call", payload);
        webRtcClient.endCallAndRemoveRemoteStream();
        showFriendsAddClusterHideEndAndEndBusyCall();
        handler.removeCallbacksAndMessages(null);
    }

    @OnClick(R.id.end_busy_call_button)
    public void onEndBusyCallBtnClick() {
        webRtcClient.endCallAndRemoveRemoteStream();
        showFriendsAddClusterHideEndAndEndBusyCall();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @OnClick(R.id.video_toggle)
    /* package-local */ void toggleVideo() {
        if (videoVisible) {
            webRtcClient.stopVideoSource();
            webRtcClient.removeVideoSource();
            videoDisabledButton.setVisibility(View.VISIBLE);
        } else {
            webRtcClient.restartVideoSource();
            webRtcClient.addVideoSource();
            videoDisabledButton.setVisibility(View.INVISIBLE);
        }
        videoVisible = !videoVisible;
    }

    private void updateVideoViews() {
        MyLog.i(LOG_TAG, "Post to Updating video Views");
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (nuggetInjector.isOngoingCall()) {
                    MyLog.i(LOG_TAG, "On Going call Updating video Views");
                    VideoRendererGui.update(remote, REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                            scalingType, true);
                    VideoRendererGui.update(local, LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT,
                            scalingType, true);
                } else {
                    MyLog.i(LOG_TAG, "NO call Updating video Views");
                    VideoRendererGui.update(local, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                            LOCAL_HEIGHT_CONNECTING, scalingType, true);
                }
            }
        });
    }

    @OnClick(R.id.add_friends_to_chat)
    /* package-local */ void addFriendsForCall() {
        Intent intent = new Intent(gamesChatActivity, FriendsManagerActivity.class);
        intent.putExtra("user_id", "dummy");
        nuggetInjector.logEvent(AnalyticConstants.ADD_FRIENDS_TO_CHAT_BUTTON_CLICKED,
                null /* bundle */);
        startActivityForResult(intent, 1234);
    }

    @OnClick({R.id.popular_friend_1})
    /* package-local */ void callFavFriend1() {
        nuggetInjector.logEvent(AnalyticConstants.POPULAR_FRIEND_1_BUTTON_CLICKED,
                null /* bundle */);
        sendPreCallHandshake(SharedPreferenceUtility.getFavFriend1(gamesChatActivity));
    }

    @OnClick({R.id.popular_friend_2})
    /* package-local */ void callFavFriend2() {
        nuggetInjector.logEvent(AnalyticConstants.POPULAR_FRIEND_2_BUTTON_CLICKED,
                null /* bundle */);
        sendPreCallHandshake(SharedPreferenceUtility.getFavFriend2(gamesChatActivity));
    }

    private void undbindService() {
        if (isBound) {
            if (chatService != null) {
                chatService.unregisterEventListener(this);
            }
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void fetchDataForGames(final Context context) {
        String firebaseMultiPlayerGamesUri = Conf.firebaseMultiPlayerGamesUri();
        MyLog.i(LOG_TAG, "Fetching MultiPlayer Games Stream : , " + firebaseMultiPlayerGamesUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseMultiPlayerGamesUri);

        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot itemDataSnapshot : dataSnapshot.getChildren()) {
                    String id = itemDataSnapshot.getKey();
                    multiplayerIDList.add(0, id);
                    MyLog.d(LOG_TAG, ">>>multiplayer id: " + id);
                }
                fetchMultiplayerGames(context);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void fetchMultiplayerGames(final Context context) {
        String firebaseUri = Conf.firebaseGamesUri();
        MyLog.i(LOG_TAG, "Fetching Games Stream : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        for (int i = 0; i < multiplayerIDList.size(); i++) {
            String gameID = multiplayerIDList.get(i);
            MyLog.d(LOG_TAG, ">>multi ids: " + gameID);
            firebaseRef.child(gameID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    GamesData gamesData = snapshot.getValue(GamesData.class);
                    if(gamesData != null) {
                        GamesItem gamesItem = new GamesItem(gamesData.getDataId(), gamesData.getTitle(),
                                gamesData.getFeaturedImage(), gamesData.getUrl(), gamesData.getPortrait(), false, false, gamesData.getValueScore());

                        multiplayerGamesItemList.add(0,gamesItem);
                        MyLog.d(LOG_TAG,">>> Multi game added: " + gamesData.getDataId());
                        setUpListView(gamesItem);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                }
            });
        }
    }


    private void setUpListView(GamesItem gamesItem) {
        LinearLayout gamesList = (LinearLayout) view.findViewById(R.id.games_list);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.multiplayer_item, gamesList, false);
        TextView textView = (TextView) view.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView) view.findViewById(R.id.grid_image);
        MyLog.i(LOG_TAG, "multiplayer game name, " + gamesItem.getGamesName());
        MyLog.i(LOG_TAG, "multiplayer game image, " + gamesItem.getGamesImage());

        textView.setText(gamesItem.getGamesName());
        String imageURl = Conf.CLOUDINARY_PREFIX_URL + gamesItem.getGamesImage();
        MyLog.d("The image uri ", imageURl);
        GlideUtils.loadImage(gamesChatActivity, imageView, null, imageURl);

        view.setOnClickListener(new MultiPlayerClickListener(gamesItem));
        gamesList.addView(view);

    }

    @Override
    public void onShowFragment() {
        MyLog.d(LOG_TAG, "onShowFragment: Chat Fragment shown");
        if (videoCallView != null) {
            MyLog.d(LOG_TAG, "ChatFragment shown... show local stream");
            if (videoVisible) {
                webRtcClient.restartVideoSource();
            }
            if (videoCallView.getVisibility() == View.INVISIBLE ){
                videoCallView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onHideFragment() {
        MyLog.i(LOG_TAG, "onHideFragment: Chat Fragment ");
        if (videoCallView != null) {
            MyLog.d(LOG_TAG, "ChatFragment hidden... hide local stream");
            webRtcClient.stopVideoSource();
            videoCallView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onScrollFragment(int position, int postionOffsetPixels) {
        MyLog.i(LOG_TAG, "onScrollFragment: Chat Fragment " + position);
    }

    private void initWebRtc(String myUserId) {
        Point displaySize = new Point();
        gamesChatActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                false /*loopback*/, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true
        );
        String iceServersString = SharedPreferenceUtility.getIceServersUrls(gamesChatActivity);
        webRtcClient = new WebRtcClient(this, params,
                VideoRendererGui.getEglBaseContext(), myUserId, iceServersString,
                gamesChatActivity);
    }

    @Override
    public void onStatusChanged(String newStatus) {
        MyLog.i(LOG_TAG, "On Status Changed: " + newStatus);
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        MyLog.i(LOG_TAG, "onLocalStream");
        if (!localStream.videoTracks.isEmpty()) {
            localRenderer = new VideoRenderer(local);
            localStream.videoTracks.get(0).addRenderer(localRenderer);
            updateVideoViews();
        } else {
            MyLog.w(LOG_TAG, "Video tracks empty");
        }
    }

    @Override
    public void onLocalStreamFirstFrame() {
        if (videoCallView.getVisibility() == View.INVISIBLE) {
            MyLog.i(LOG_TAG, "Show video call view");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    videoCallView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onRemoveLocalStream(MediaStream localStream) {
        MyLog.i(LOG_TAG, "onRemoveLocalStream");
        if (localStream != null && localStream.videoTracks != null && !localStream.videoTracks.isEmpty() && localRenderer != null) {
            localStream.videoTracks.get(0).removeRenderer(localRenderer);
        }
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        MyLog.i(LOG_TAG, "inside onAddRemoteStream");
        if (gamesChatActivity == null) {
            MyLog.e(LOG_TAG, "activity game chat destroyed");
            return;
        }
        ViewUtils.setWindowImmersive(gamesChatActivity.getWindow(), mainHandler);
        hideFriendsAddCluster();
        nuggetInjector.setOngoingCall(true);
        setLoudSpeakerOn();
        handler.removeCallbacksAndMessages(null);
        if (!remoteStream.videoTracks.isEmpty()) {
            MyLog.i(LOG_TAG, "remote stream not empty");
            remoteVideoRender = new VideoRenderer(remote);
            remoteStream.videoTracks.get(0).addRenderer(remoteVideoRender);
            updateVideoViews();
        } else {
            MyLog.w(LOG_TAG, "Remote video tracks empty");
        }
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream) {
        MyLog.i(LOG_TAG, "on Remove Remote stream");
        if (gamesChatActivity == null) {
            MyLog.e(LOG_TAG, "onRemove activity game chat destroyed");
            return;
        }
        resetAudioManager();
        MyLog.d(LOG_TAG, "onRemoveRemoteStream: abandon audio focus");
        audioManager.abandonAudioFocus(null);
        ViewUtils.showWindowNavigation(gamesChatActivity.getWindow(), mainHandler);
        nuggetInjector.setOngoingCall(false);
        if (remoteStream != null && !remoteStream.videoTracks.isEmpty()) {
            if (remoteVideoRender != null) {
                remoteStream.videoTracks.get(0).removeRenderer(remoteVideoRender);
                remoteVideoRender = null;
            }
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                multiplayerGamesView.setVisibility(View.INVISIBLE);
                textPlayWithFriends.setVisibility(View.INVISIBLE);
            }
        });
        if (webRtcClient != null) {
            webRtcClient.setCameraAndUpdateVideoViews();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLog.i(LOG_TAG, "onResume" + (isAdded() && isVisible() && getUserVisibleHint()));
        videoCallView.onResume();
        if (webRtcClient != null && videoVisible) {
            webRtcClient.onResume();
        }

        if (nuggetInjector.isOngoingCall() || nuggetInjector.isInitiator()) {
            showEndCallBtn();
        }
        if (bundle != null) {
            MyLog.d(LOG_TAG, "bundle not null " + bundle.getString("user_id"));
            if (bundle.getString("user_id") == null) {
                MyLog.d(LOG_TAG, "START CALL OnRESUME");
            }
        } else {
            MyLog.d(LOG_TAG, "bundle null");
        }
    }

    @Override
    public void onPause() {
        MyLog.i(LOG_TAG, "onPause");
        videoCallView.onPause();
        if (webRtcClient != null) {
            webRtcClient.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        MyLog.i(LOG_TAG, "onDestoryView");
        resetAudioManager();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        MyLog.i(LOG_TAG, "onDestroy" + nuggetInjector.isOngoingCall());
        if (webRtcClient != null) {
            if (nuggetInjector.isOngoingCall()) {
                JSONObject payload = new JSONObject();
                try {
                    MyLog.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
                    payload.put("from", myUserId);
                    payload.put("to", targetUserId);
                    payload.put("token", "abcd");
                    payload.put("caller", SharedPreferenceUtility.getFacebookUserName(gamesChatActivity));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (chatService != null && chatService.socket != null) {
                    MyLog.i(LOG_TAG, "MessageHandler emit end_call");
                    chatService.socket.emit("end_call", payload);
                } else {
                    String errStr = "Chat service or socket null";
                    MyLog.e(LOG_TAG, errStr);
                    throw new IllegalStateException(errStr);
                }
            }
            MyLog.i(LOG_TAG, "MessageHandler onDestroy" + nuggetInjector.isOngoingCall());
            undbindService();
            webRtcClient.releaseLocalMediaOnDestrory();
            webRtcClient.disposePeerConnnectionFactory();
        }
        releaseVideoRendererGui();
        releaseAudioAndInjector();
        super.onDestroy();
    }

    private void releaseAudioAndInjector() {
        if (nuggetInjector != null) {
            nuggetInjector.setInitiator(false);
            nuggetInjector.setOngoingCall(false);
        }
        if (audioManager != null) {
            audioPlayer.stopRingtone();
        }
    }

    private void releaseVideoRendererGui() {
        if (local != null) {
            VideoRendererGui.remove(local);
        }
        if (remote != null) {
            VideoRendererGui.remove(remote);
        }
        VideoRendererGui.dispose();
    }

    private void sendPreCallHandshake(String facebookId) {
        nuggetInjector.setInitiator(true);
        targetUserId = facebookId;
        webRtcClient.addFriendForChat(facebookId, chatService.socket);

        JSONObject payload = new JSONObject();
        try {
            payload.put("from", myUserId);
            payload.put("to", targetUserId);
            payload.put("token", "abcd");
            MyLog.e(LOG_TAG, "MessageHandler emit pre_call_handshake " + payload.toString());
            chatService.socket.emit("pre_call_handshake", payload);
        } catch (JSONException e) {
            MyLog.e(LOG_TAG, e.getMessage());
        }

        SharedPreferenceUtility.setFavouriteFriend(getActivity(), facebookId);
        triggerImageChanges();
        audioPlayer.playRingtone(AudioPlayer.RINGTONE);
        endCall.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.INVISIBLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onEndCallBtnClick();
            }
        }, 30000);
    }

    private void triggerImageChanges() {
        String friend1 = SharedPreferenceUtility.getFavFriend1(getActivity());
        String friend2 = SharedPreferenceUtility.getFavFriend2(getActivity());
        if (!friend1.equals("")) {
            String friendOnePicUrl = UserInfo.getUserPic(friend1);
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
            String friendTwoPicUrl = UserInfo.getUserPic(friend2);
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                multiplayerGamesView.setVisibility(View.VISIBLE);
                textPlayWithFriends.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showFriendsAddClusterHideEndAndEndBusyCall() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endCall.setVisibility(View.INVISIBLE);
                multiplayerGamesView.setVisibility(View.INVISIBLE);
                textPlayWithFriends.setVisibility(View.INVISIBLE);
                endBusyCallBtn.setVisibility(View.INVISIBLE);
                audioPlayer.stopRingtone();
                MyLog.d(LOG_TAG, "Show Friends Add Clusters");
                linearLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void getUserFriends() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        MyLog.d(LOG_TAG, response.toString());
                        JSONObject object = response.getJSONObject();
                        try {
                            for (int i = 0; i < object.getJSONArray("data").length(); i++) {
                                JSONObject dataObject = (object.getJSONArray("data")).getJSONObject(i);
                                MyLog.d(LOG_TAG, dataObject.toString());
                                String name = dataObject.getString("name");
                                String userId = dataObject.getString("id");
                                FriendInfo userData = new FriendInfo();
                                userData.setFacebookId(userId);
                                userData.setName(name);
                                selectUsers.add(userData);
                                MyLog.d(LOG_TAG, "Values " + name + "  " + userId);
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
        MyLog.i(LOG_TAG, "fragment onActivityResult");
        if (data != null) {
            if (requestCode == 1234) {
                MyLog.i(LOG_TAG, "before toast onActivityResult");
                requestAudioFocus();
                showEndCallBtn();
                sendPreCallHandshake(data.getStringExtra("user_id"));
            } else if (requestCode == ChatFragment.INCOMING_CALL_CODE) {
                Bundle receivedBundle = data.getExtras();
                Bundle newReqBundle = new Bundle();
                newReqBundle.putBundle("requestBundle", receivedBundle);
                boolean accepted = receivedBundle.getBoolean(IncomingCallActivity.CALL_ACCEPTED);
                if (accepted) {
                    MyLog.i(LOG_TAG, "User call accepted");
                    acknowledgePreCallHandshake(newReqBundle);
                } else {
                    MyLog.i(LOG_TAG, "User call rejected");
                    rejectCall(newReqBundle);
                }
            }
        }
    }

    private void requestAudioFocus() {
        int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            MyLog.d(LOG_TAG, "audio focus granted");
        } else {
            MyLog.d(LOG_TAG, "audio focus not granted");
        }
    }

    public void sendPreCallHandshakeComplete(JSONObject data) {
        MyLog.i(LOG_TAG, "Peer sendPreCallHandshakeComplete");
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

                    MyLog.e(LOG_TAG, "MessageHandler emit handshake_complete");
                    chatService.socket.emit("handshake_complete", payload);
                    showEndCallBtn();
                }
            } catch (JSONException e) {
                MyLog.e(LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public void onPreCallHandshake(JSONObject data) {
        MyLog.i(LOG_TAG, "Peer onPreCallHandshake");
        try {
            String from = data.getString("from");
            String to = data.getString("to");
            String token = data.getString("token");
            String type = "pre_call_handshake";
            MyLog.e(LOG_TAG, from + "::" + to + "::" + token + "::" + nuggetInjector.isIncomingCall() + "::" + nuggetInjector.toString());

            if (webRtcClient.isInitiator() || nuggetInjector.isOngoingCall() || nuggetInjector.isIncomingCall()) {
                data.put("from", to);
                data.put("to", from);
                chatService.socket.emit("ongoing_call", data);
                return;
            }

            if (!webRtcClient.isInitiator() && myUserId.equals(to) && "abcd".equals(token)) {
                targetUserId = data.getString("from");
                Intent intent = new Intent(getActivity(), IncomingCallActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("to", to);
                bundle.putString("from", from);
                bundle.putString("token", token);
                bundle.putString("type", type);
                bundle.putString("from_activity", "chat_frag");
                intent.putExtras(bundle);
                startActivityForResult(intent, INCOMING_CALL_CODE);
            }
        } catch (JSONException e) {
            MyLog.e(LOG_TAG, e.getMessage());
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
                    MyLog.e(LOG_TAG, "when handshake complete... create offer");
                }
            } catch (JSONException e) {
                MyLog.e(LOG_TAG, e.getMessage());
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
        MyLog.i(LOG_TAG, "onCallRequestOrAnswer sdp" + sdp.type);
        Peer peer = webRtcClient.getPeer();
        if (peer != null) {
            MyLog.i(LOG_TAG, "Peer not null.. going to set remote sdp");
            peer.getPeerConnection().setRemoteDescription(peer, sdp);
            showEndCallBtn();
        }
        audioPlayer.stopRingtone();
    }

    @Override
    public void onGameLink(String link) {
        // launch the WebView
        MyLog.i(LOG_TAG, "Received game link " + link);
        gamesChatActivity.launchGameActivity(link, true /*isPortrait*/, true /*isMultiplayer*/, myUserId,
                targetUserId);
    }

    @Override
    public void onCallEnd() {
        MyLog.i(LOG_TAG, "MessageHandler onCallEnd");
        webRtcClient.endCallAndRemoveRemoteStream();
        Intent intent = new Intent("com.nuggetchat.messenger.DISMISS_INCOMING_CALL_ACTIVITY");
        getActivity().sendBroadcast(intent);
        //hideEndCallBtn();
        showFriendsAddClusterHideEndAndEndBusyCall();
    }

    @Override
    public void onFetchIceCandidates(IceCandidate candidate) {
        MyLog.i(LOG_TAG, "onFetchIceCandidates");
        boolean candidateQueued = webRtcClient.lockAndQueueRemoteCandidates(candidate);
        if (!candidateQueued) {
            MyLog.i(LOG_TAG, "Directly add to peer ice candidates after connection");
            webRtcClient.addIceCandidateToPeerConnection(candidate);
        }
    }

    @Override
    public void onCallRejected() {
        showFriendsAddClusterHideEndAndEndBusyCall();
        // hideEndCallBtn();
        webRtcClient.endCallAndRemoveRemoteStream();
    }

    @Override
    public void onCallOngoing() {
        MyLog.i(LOG_TAG, "on call ongoing");
        showEndBusyCallBtn();
        userBusyToast();
    }

    private void showEndBusyCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endBusyCallBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideEndBusyCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endBusyCallBtn.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void userBusyToast() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                audioPlayer.playRingtone(AudioPlayer.BUSYTONE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showFriendsAddClusterHideEndAndEndBusyCall();
                        webRtcClient.endCallAndRemoveRemoteStream();
                    }
                }, 3000);
                Toast.makeText(ChatFragment.this.getActivity(), "User is busy.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideEndCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                endCall.setVisibility(View.INVISIBLE);
                audioPlayer.stopRingtone();
                //startCallButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showEndCallBtn() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MyLog.i(LOG_TAG, "the end call button");
                endCall.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
                //startCallButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void acknowledgePreCallHandshake(Bundle bundle) {
        MyLog.e(LOG_TAG, "acknowledgePreCallHandshake - received pre call handshake, sending acknowledgement");
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

            sendPreCallHandshakeComplete(requestData);
        } catch (JSONException e) {
            MyLog.e(LOG_TAG, e.getMessage());
        }
    }

    private void rejectCall(Bundle bundle) {
        MyLog.e(LOG_TAG, "Reject Call - received pre call handshake, sending rejection");
        Bundle requestBundle = bundle.getBundle("requestBundle");
        if (requestBundle == null) {
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("from", requestBundle.get("to"));
            jsonObject.put("to", requestBundle.get("from"));
            jsonObject.put("token", requestBundle.get("token"));

            chatService.socket.emit("reject_call", jsonObject);
        } catch (JSONException e) {
            MyLog.e(LOG_TAG, e.getMessage());
        }
        handler.removeCallbacksAndMessages(null);
    }

    private class MultiPlayerClickListener implements View.OnClickListener {
        private GamesItem gamesItem;

        public MultiPlayerClickListener(GamesItem item) {
            gamesItem = item;
        }

        @Override
        public void onClick(View view) {
            if (nuggetInjector.isOngoingCall()) {
                nuggetInjector.logEvent(AnalyticConstants.MULTIPLAYER_GAMES_BUTTON_CLICKED,
                        null /* bundle */);
                String gameSessionId = UUID.randomUUID().toString();
                String thisGameUrl = gamesItem.getGamesUrl()
                        + "?room=" + gameSessionId
                        + "&user=" + "ann";
                String peerGameUrl = gamesItem.getGamesUrl()
                        + "?room=" + gameSessionId
                        + "&user=" + "dan";
                gamesChatActivity.launchGameActivity(thisGameUrl, gamesItem.getPortrait(), true /*isMultiplayer*/,
                        myUserId, targetUserId);
                // emit to peer
                JSONObject payload = new JSONObject();
                try {
                    MyLog.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
                    payload.put("from", myUserId);
                    payload.put("to", targetUserId);
                    payload.put("token", "abcd");
                    payload.put("gameID",gamesItem.getGameKey());
                    payload.put("game_link", peerGameUrl);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MyLog.i(LOG_TAG, "Emit game link " + peerGameUrl);
                chatService.socket.emit("game_link", payload);
                Log.d(LOG_TAG,">>>Update Sender Score");
                gamesChatActivity.updateScore(myUserId,targetUserId);

            } else {
                Toast.makeText(getActivity(), "Please call a friend to start playing multiplayer!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
