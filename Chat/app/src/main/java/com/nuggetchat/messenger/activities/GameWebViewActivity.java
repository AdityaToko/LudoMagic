package com.nuggetchat.messenger.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nuggetchat.messenger.R;


public class GameWebViewActivity extends AppCompatActivity {
    private static final String EXTRA_GAME_URL = GameWebViewActivity.class.getName() + ".game_url";
    private WebView gameWebView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.games_web_view_activity);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String gameUrl = bundle.getString(EXTRA_GAME_URL);

        gameWebView = (WebView) findViewById(R.id.game_web_view);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (gameWebView != null) {
            gameWebView.destroy();
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

}
