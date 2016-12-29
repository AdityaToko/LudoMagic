package com.nuggetchat.messenger.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.datamodel.GamesData;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.rtcclient.Peer;
import com.nuggetchat.messenger.rtcclient.PeerConnectionParameters;
import com.nuggetchat.messenger.rtcclient.RtcListener;
import com.nuggetchat.messenger.rtcclient.WebRtcClient;
import com.nuggetchat.messenger.utils.GlideUtils;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.tokostudios.chat.ChatService;
import com.tokostudios.chat.User;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;

public class ChatFragment extends Fragment implements RtcListener, EventListener {
    private static final String LOG_TAG = ChatFragment.class.getSimpleName();
    private static final int LOCAL_X = 72;
    private static final int LOCAL_Y = 72;
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
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView rtcView;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private WebRtcClient webRtcClient;
    private String socketAddress;
    private ImageView startCallButton;
    private ImageView endCall;
    private String targetId;
    private User user1;
    private View view;
    private ArrayList<String> multiPlayerGamesName;
    private ArrayList<String> multiPlayerGamesImage;
    private ArrayList<GamesItem> gamesItemList;
    ArrayList<String> gamesName;
    ArrayList<String> gamesImage;
    private NuggetApplication application;
    private ChatService chatService;
    private boolean isBound;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            chatService = ((ChatService.ChatBinder)iBinder).getService();
            chatService.registerEventListener(ChatFragment.this);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getActivity().getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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
        gamesName = new ArrayList<>();
        gamesImage = new ArrayList<>();
        gamesItemList = new ArrayList<>();
        application = (NuggetApplication) getActivity().getApplicationContext();
        fetchData();

        Intent intent = getActivity().getIntent();
        targetId = intent.getStringExtra("userId");

        startCallButton = (ImageView) view.findViewById(R.id.start_call_button);
        endCall = (ImageView) view.findViewById(R.id.end_call_button);
        linearLayout.setVisibility(View.VISIBLE);
        getUserFriends();
        socketAddress = "http://192.168.0.118:5000/";

        String userId = SharedPreferenceUtility.getFacebookUserId(getActivity());
        String username = SharedPreferenceUtility.getFacebookUserName(getActivity());
        Log.e(LOG_TAG, "User is : " + userId + " " + username);
        user1 = new User(userId, username);
        Log.e(LOG_TAG, "User is : " + userId + " " + username);
        rtcView = (GLSurfaceView) view.findViewById(R.id.glview_call);
        rtcView.setPreserveEGLContextOnPause(true);
        rtcView.setKeepScreenOn(true);
        String friend1 = SharedPreferenceUtility.getFavFriend1(getActivity());
        String friend2 = SharedPreferenceUtility.getFavFriend2(getActivity());
        if (!friend1.equals("")) {
            GlideUtils.loadImage(getActivity(), popularFriend1, null,
                    "https://graph.facebook.com/" + friend1 + "/picture?width=200&height=150");
        }
        if (!friend2.equals("")) {
            GlideUtils.loadImage(getActivity(), popularFriend2, null,
                    "https://graph.facebook.com/" + friend2 + "/picture?width=200&height=150");
        }

        user1 = new User(userId, username);
        VideoRendererGui.setView(rtcView, new Runnable() {
            @Override
            public void run() {
                init(user1, targetId);
                getActivity().startService(new Intent(getActivity(), ChatService.class));
                getActivity().bindService(new Intent(getActivity(), ChatService.class), serviceConnection,
                        Context.BIND_AUTO_CREATE);
                isBound = true;
            }
        });

        remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, false);

        localRender = VideoRendererGui.create(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT, scalingType,
                false);

        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFriendsDialog();
                startCallButton.setVisibility(View.INVISIBLE);
                endCall.setVisibility(View.VISIBLE);
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
               // webRtcClient.endCall();
                showFriendsAddCluster();
                VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                        LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
            }
        });

        return view;
    }

    @OnClick(R.id.add_friends_to_chat)
    /* package-local */ void addFriendsForCall() {
        //showFriendsDialog();
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
                //Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getValue());
                GamesData gamesDate = dataSnapshot.getValue(GamesData.class);
                Log.i(LOG_TAG, "the data id, " + gamesDate.getTitle());

                gamesName.add(gamesDate.getTitle());
                gamesImage.add(gamesDate.getFeaturedImage());
                GamesItem gamesItem = new GamesItem(dataSnapshot.getKey(), gamesDate.getTitle(),
                        gamesDate.getFeaturedImage());
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
                //Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                for (int i = 0 ; i < gamesItemList.size(); i++) {
                    Log.i(LOG_TAG, "games key " + gamesItemList.get(i).getGameKey());
                    if (dataSnapshot.getKey().equals(gamesItemList.get(i).getGameKey())) {
                        Log.i(LOG_TAG, "dataSnapshot games key " + dataSnapshot.getKey());
                        Log.i(LOG_TAG, "games name, " + gamesItemList.get(i).getGamesName());
                        Log.i(LOG_TAG, "games Image, " + gamesItemList.get(i).getGamesImage());
                        multiPlayerGamesName.add(gamesItemList.get(i).getGamesName());
                        multiPlayerGamesImage.add(gamesItemList.get(i).getGamesImage());
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

        gamesList.addView(view);
    }

    private void startCall() {
        webRtcClient.setInitiator(true);
        webRtcClient.createOffer(webRtcClient.peers.get(0));
    }

    private void init(User user1, String targetId) {
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true
        );
        String iceServersString = SharedPreferenceUtility.getIceServersUrls(getActivity());
        webRtcClient = new WebRtcClient(this, params,
                VideoRendererGui.getEGLContext(), user1, iceServersString,
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
        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        Log.e(LOG_TAG, "inside onAddRemoteStream");
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, true);
        VideoRendererGui.update(localRender, LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT,
                scalingType, true);
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream) {
        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        rtcView.onResume();
        if (webRtcClient != null) {
            webRtcClient.onResume();
        }
        if (bundle != null) {
            Log.d(LOG_TAG, "bundle not null " + bundle.getString("user_id"));
            if (bundle.getString("user_id") == null) {
                endCall.setVisibility(View.INVISIBLE);
                multiplayerGamesView.setVisibility(View.INVISIBLE);
                startCallButton.setVisibility(View.VISIBLE);
                showFriendsAddCluster();
            }
        } else {
            Log.d(LOG_TAG, "bundle null");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        rtcView.onPause();
        if (webRtcClient != null) {
            webRtcClient.onPause();
        }

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
            chatService.socket.emit("end_call", payload);
            webRtcClient.endCall();
            undbindService();
        }
    }


    private void showFriendsDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setTitle("Choose a friend");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                endCall.setVisibility(View.INVISIBLE);
                startCallButton.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FriendInfo user = (FriendInfo) adapter.getItem(which);
                startFriendCall(user.getFacebookId());
            }
        });
        builderSingle.show();
    }

    private void startFriendCall(String facebookId) {
        webRtcClient.setInitiator(true);
        application.setInitiator(true);
        webRtcClient.addFriendForChat(facebookId, chatService.socket);
        webRtcClient.createOffer(webRtcClient.peers.get(0));
        hideFriendsAddCluster();
        SharedPreferenceUtility.setFavouriteFriend(getActivity(), facebookId);
    }

    private void hideFriendsAddCluster() {
        endCall.setVisibility(View.VISIBLE);
        startCallButton.setVisibility(View.INVISIBLE);
        multiplayerGamesView.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.INVISIBLE);
    }

    private void showFriendsAddCluster() {
        endCall.setVisibility(View.INVISIBLE);
        multiplayerGamesView.setVisibility(View.INVISIBLE);
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
        if (requestCode == 1234) {
            Log.d(LOG_TAG, "before toast onActivityResult");
            hideFriendsAddCluster();
            if (data != null) {
                Toast.makeText(getActivity(), data.getStringExtra("user_id"), Toast.LENGTH_LONG).show();
                startFriendCall(data.getStringExtra("user_id"));
            }
        }
    }

    @Override
    public void onCall(String userId, Socket socket) {
        if (!webRtcClient.isInitiator()) {
            webRtcClient.addFriendForChat(userId, socket);
        }
    }

    @Override
    public void onCallRequestOrAnswer(SessionDescription sdp) {
        Peer peer = webRtcClient.peers.get(0);
        peer.getPeerConnection().setRemoteDescription(peer, sdp);
    }

    @Override
    public void onCallEnd() {
        webRtcClient.endCall();
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
}
