package com.nuggetchat.messenger.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.UserFriendsAdapter;
import com.nuggetchat.messenger.datamodel.UserDetails;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.tokostudios.chat.User;
import com.tokostudios.chat.webRtcClient.PeerConnectionParameters;
import com.tokostudios.chat.webRtcClient.RtcListener;
import com.tokostudios.chat.webRtcClient.WebRtcClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;
import java.util.List;

public class ChatFragmet extends Fragment implements RtcListener  {
    private static final String LOG_TAG = ChatFragmet.class.getSimpleName();
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView rtcView;

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
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;

    private WebRtcClient webRtcClient;
    private String socketAddress;
    private ImageView startCallButton;
    private ImageView endCall;
    private String targetId;
    private User user1;
    ArrayList<UserDetails> selectUsers = new ArrayList<>();
    UserFriendsAdapter adapter;

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
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        Intent intent = getActivity().getIntent();
        targetId = intent.getStringExtra("userId");

        startCallButton = (ImageView) view.findViewById(R.id.start_call_button);
        endCall = (ImageView) view.findViewById(R.id.end_call_button);
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

        user1 = new User(userId, username);
        VideoRendererGui.setView(rtcView, new Runnable() {
            @Override
            public void run() {
                init(user1, targetId);
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
                startCallButton.setImageResource(R.drawable.end_call_button);
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
                webRtcClient.socket.emit("end_call", payload);
                webRtcClient.endCall();
                VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                        LOCAL_HEIGHT_CONNECTING, scalingType, true);
            }
        });

        return view;
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

        webRtcClient = new WebRtcClient(this, socketAddress, params,
                VideoRendererGui.getEGLContext(), user1, getActivity());
        //startCall();
    }

    public void startCam() {
        // Camera settings
        webRtcClient.start("Aman");
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
        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                LOCAL_HEIGHT_CONNECTING, scalingType, true);
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
    public void onRemoveRemoteStream() {
        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        rtcView.onResume();
        if (webRtcClient != null) {
            webRtcClient.onResume();
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
            webRtcClient.onDestroy();
        }
    }


    private void showFriendsDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setTitle("Choose a friend");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserDetails user = (UserDetails) adapter.getItem(which);
                String userId = user.getUserId();
                webRtcClient.setInitiator(true);
                webRtcClient.addFriendForChat(userId);
                webRtcClient.createOffer(webRtcClient.peers.get(0));
            }
        });
        builderSingle.show();
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
                                UserDetails userData = new UserDetails();
                                userData.setUserId(userId);
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
}
