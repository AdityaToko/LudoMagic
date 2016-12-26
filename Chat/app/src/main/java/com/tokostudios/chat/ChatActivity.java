package com.tokostudios.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.tokostudios.chat.webRtcClient.PeerConnectionParameters;
import com.tokostudios.chat.webRtcClient.RtcListener;
import com.tokostudios.chat.webRtcClient.WebRtcClient;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class ChatActivity extends AppCompatActivity implements RtcListener {

    private static final String LOG_TAG = ChatActivity.class.getSimpleName();
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
    private Button button;
    private Button endCall;

    private User user1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_chat);
        button = (Button) findViewById(R.id.start_call_button);
        endCall = (Button) findViewById(R.id.end_call);

        socketAddress = "http://192.168.0.118:5000/";

        rtcView = (GLSurfaceView) findViewById(R.id.glview_call);
        rtcView.setPreserveEGLContextOnPause(true);
        rtcView.setKeepScreenOn(true);
        String userId  = this.getSharedPreferences("MyPrefs",Context.MODE_PRIVATE).getString("userId","");
        if("".equals(userId) || userId == null) {
            user1 = new User(WebRtcClient.getRandomString(), WebRtcClient.getRandomString());
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userId", user1.getId());
            editor.putString("username", user1.getName());
            editor.apply();
        }
        String userName = this.getSharedPreferences("MyPrefs",Context.MODE_PRIVATE).getString("username","");
        user1 = new User(userId, userName);

        VideoRendererGui.setView(rtcView, new Runnable() {
            @Override
            public void run() {
                init(user1);
            }
        });

        remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, false);

        localRender = VideoRendererGui.create(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT, scalingType,
                false);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webRtcClient.setInitiator(true);
                webRtcClient.createOffer(webRtcClient.peers.get(0));
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webRtcClient.socket.emit("message","end_call");
                webRtcClient.endCall();
            }
        });

    }

    private void init(User user1) {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true
        );

        webRtcClient = new WebRtcClient(this, socketAddress, params, VideoRendererGui.getEGLContext(), user1);

    }

    public void startCam() {
        // Camera settings
        webRtcClient.start("Aman");
    }

    @Override
    public void onCallReady(String callId) {
        startCam();
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
    protected void onResume() {
        super.onResume();
        rtcView.onResume();
        if (webRtcClient != null) {
            webRtcClient.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        rtcView.onResume();
        if (webRtcClient != null) {
            webRtcClient.onPause();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webRtcClient != null) {
            webRtcClient.onDestroy();
        }
    }
}
