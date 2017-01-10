package com.nuggetchat.messenger.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.ViewUtils;


public class GameWebViewActivity extends AppCompatActivity {
    public static final String EXTRA_GAME_URL = GamesFragment.class.getName() + ".game_url";
    public static final String EXTRA_GAME_ORIENTATION = GamesFragment.class.getName() + ".game_orientation";
    private static final String LOG_TAG = GameWebViewActivity.class.getSimpleName();
    private WebView gameWebView;
    private NuggetInjector nuggetInjector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.games_web_view_activity);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String gameUrl = bundle.getString(EXTRA_GAME_URL);
        nuggetInjector = NuggetInjector.getInstance();
        Boolean portrait = null;
        if (bundle.containsKey(EXTRA_GAME_ORIENTATION)) {
            portrait = bundle.getBoolean(EXTRA_GAME_ORIENTATION);
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

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "On resume - game webview");
        // Finish only on solo games. TODO
        if (nuggetInjector.isOngoingCall()) {
            Log.i(LOG_TAG, "On resume - game webview - Finishing ");
            finish();
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

}
