package com.nuggetchat.messenger.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.services.ChatService;
import com.nuggetchat.messenger.chat.MessageHandler;
import com.nuggetchat.messenger.rtcclient.GameLeftListener;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;


public class GameWebViewActivity extends AppCompatActivity implements GameLeftListener {
    public static final String EXTRA_GAME_URL = GamesFragment.class.getName() + ".game_url";
    public static final String EXTRA_GAME_ORIENTATION = GamesFragment.class.getName() + ".game_orientation";
    public static final String EXTRA_GAME_IS_MULTIPLAYER = GameWebViewActivity.class.getName() + ".game_is_multiplayer";
    public static final String EXTRA_FROM = GameWebViewActivity.class.getName() + ".from";
    public static final String EXTRA_TO = GameWebViewActivity.class.getName() + ".to";
    private static final String LOG_TAG = GameWebViewActivity.class.getSimpleName();
    private WebView gameWebView;
    private NuggetInjector nuggetInjector;
    private Boolean gameIsMultiplayer;
    private ChatService chatService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            chatService = ((ChatService.ChatBinder) iBinder).getService();
            MessageHandler messageHandler = chatService.getMessageHandler();
            if (messageHandler != null) {
                messageHandler.setGameLeftListener(GameWebViewActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            chatService = null;
        }
    };
    private boolean isBound;
    private String myUserId;
    private String targetUserId;
    private boolean hasLeftGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindChatService();
        setContentView(R.layout.games_web_view_activity);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String gameUrl = bundle.getString(EXTRA_GAME_URL);
        myUserId = bundle.getString(EXTRA_FROM);
        targetUserId = bundle.getString(EXTRA_TO);
        nuggetInjector = NuggetInjector.getInstance();
        nuggetInjector.getMixpanel().logCreateView(LOG_TAG);
        Boolean portrait = null;
        if (bundle.containsKey(EXTRA_GAME_ORIENTATION)) {
            portrait = bundle.getBoolean(EXTRA_GAME_ORIENTATION);
        }
        gameIsMultiplayer = false;
        if (bundle.containsKey(EXTRA_GAME_IS_MULTIPLAYER)) {
            gameIsMultiplayer = bundle.getBoolean(EXTRA_GAME_IS_MULTIPLAYER);
            MyLog.i(LOG_TAG, "Game is multiplayer" + gameIsMultiplayer);
        }
        gameWebView = (WebView) findViewById(R.id.game_web_view);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            /*Disabling hardware acceleration*/
            gameWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        gameWebView.getSettings().setJavaScriptEnabled(true);
        gameWebView.getSettings().setDomStorageEnabled(true);
        gameWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        gameWebView.setWebViewClient(new WebViewClient() {
            @Override
            @TargetApi(16)
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            @TargetApi(21)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        gameWebView.loadUrl(gameUrl);
        if (portrait == null) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            if (portrait) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    private void bindChatService() {
        Log.i(LOG_TAG, " Binding service ");
        bindService(new Intent(this, ChatService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLog.i(LOG_TAG, "On resume - game webview");
        // Finish only on solo games. TODO
        MyLog.i(LOG_TAG, "is on going call, " + nuggetInjector.isOngoingCall());
        if (nuggetInjector.isOngoingCall() && (!gameIsMultiplayer)) {
            MyLog.i(LOG_TAG, "On resume - game webview - Finishing ");
            finish();
            return;
        }

        if (gameWebView != null) {
            gameWebView.resumeTimers();
            gameWebView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (gameWebView != null) {
            gameWebView.pauseTimers();
            gameWebView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        undbindService();
        if (gameIsMultiplayer && !hasLeftGame){
            chatService.socket.emit("game_left", getPayload());
            hasLeftGame = true;
        }
        if (gameWebView != null) {
            gameWebView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ViewUtils.setWindowImmersive(getWindow());
        }
    }

    @Override
    public void onBackPressed() {
        if (gameIsMultiplayer) {
            chatService.socket.emit("game_left", getPayload());
            hasLeftGame = true;
        }
        super.onBackPressed();
    }

    private void undbindService() {
        if (isBound) {
            if (chatService != null) {
                chatService.getMessageHandler().removeGameLeftListener(this);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void showGameLeftDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Oh, Snap!")
                .setMessage("The other player has left the game!")
                .setPositiveButton("Go to Chat", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setCancelable(false);
        builder.create();
        builder.show();
    }

    private JSONObject getPayload() {
        JSONObject payload = new JSONObject();
        try {
            MyLog.e(LOG_TAG, "Users: " + myUserId + " " + targetUserId);
            payload.put("from", myUserId);
            payload.put("to", targetUserId);
            payload.put("token", "abcd");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    @Override
    public void notifyGameLeft() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showGameLeftDialog();
            }
        });
    }
}
