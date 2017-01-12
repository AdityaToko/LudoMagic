package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.FragmentChangeListener;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.chat.ChatService;
import com.nuggetchat.messenger.utils.FirebaseTokenUtils;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GamesChatActivity extends AppCompatActivity {
    private static final String LOG_TAG = GamesChatActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    /* package-local */ Toolbar toolbar;

    @BindView(R.id.tabLayout)
    /* package-local */ TabLayout gamesChatTabLayout;

    @BindView(R.id.pager)
    /* package-local */ ViewPager viewPager;

    @BindView(R.id.name_text)
    /* package-local */ TextView nameText;

    @BindView(R.id.image)
    /* package-local */ ImageView image;

    private LinearLayout tabView;
    private TextView textView;
    private ImageView imageView;
    private Intent intent;
    private Bundle requestBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyLog.i(LOG_TAG, "onCreate GameChatActivity");
        startService(new Intent(GamesChatActivity.this, ChatService.class));

        setContentView(R.layout.games_chat_activity);
        ButterKnife.bind(this);

        intent = getIntent();
        requestBundle = intent.getExtras();
        if (requestBundle != null && requestBundle.getString("from") != null) {
            MyLog.d(LOG_TAG, requestBundle.getString("from") + "");
        }

        setUpToolbar();

        setUpTabLayout();

        setUpViewPager(viewPager);

        gamesChatTabLayout.setupWithViewPager(viewPager);

        refreshFirebaseToken();

        setUpTabItems();

        if (shouldShowChatTab()) {
            showChatTab();
        } else {
            showGamesTab();
        }
        gamesChatTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();

                tabView = (LinearLayout) gamesChatTabLayout.getTabAt(position).getCustomView();
                textView = (TextView) tabView.findViewById(R.id.tab_item_text);
                imageView = (ImageView) tabView.findViewById(R.id.tab_item_image);
                if (position == 0) {
                    imageView.setImageResource(R.drawable.games_icon);
                    tabView.setBackgroundResource(R.drawable.first_tab_background);
                    textView.setTextColor(Color.parseColor("#F2830A"));
                } else {
                    imageView.setImageResource(R.drawable.video_icon);
                    tabView.setBackgroundResource(R.drawable.second_tab_background);
                    textView.setTextColor(Color.parseColor("#2290D3"));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                int position = tab.getPosition();

                tabView = (LinearLayout) gamesChatTabLayout.getTabAt(position).getCustomView();
                textView = (TextView) tabView.findViewById(R.id.tab_item_text);
                imageView = (ImageView) tabView.findViewById(R.id.tab_item_image);
                if (position == 0) {
                    tabView.setBackgroundColor(Color.parseColor("#F7F3E2"));
                } else {
                    tabView.setBackgroundColor(Color.parseColor("#F7F3E2"));
                }

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i(LOG_TAG, "onResume GamesChatActivity");
    }

    private void showGamesTab() {
        viewPager.setCurrentItem(0);
        tabView = (LinearLayout) gamesChatTabLayout.getTabAt(0).getCustomView();
        tabView.setBackgroundResource(R.drawable.first_tab_background);
    }

    private void showChatTab() {
        viewPager.setCurrentItem(1);
        tabView = (LinearLayout) gamesChatTabLayout.getTabAt(1).getCustomView();
        tabView.setBackgroundResource(R.drawable.second_tab_background);
//        tabView.requestFocus();
        MyLog.i(LOG_TAG, "chat view in focus " + tabView.hasFocus());
    }

    private boolean shouldShowChatTab() {
        return intent.getStringExtra("user_id") != null
                || (requestBundle != null
                        && "pre_call_handshake".equals(requestBundle.getString("type")));
    }

    private void setUpToolbar() {
        String userName = SharedPreferenceUtility.getFacebookUserName(this);
        MyLog.i(LOG_TAG, "the username, " + userName);
        String profilePicUrl = UserInfo.getUserPic(SharedPreferenceUtility.getFacebookUserId(this));
        Glide.with(this).load(profilePicUrl).asBitmap().centerCrop().into(new BitmapImageViewTarget(image) {
            @Override
            protected void setResource(Bitmap resource) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                image.setImageDrawable(circularBitmapDrawable);
            }
        });

        nameText.setText(userName);
        setSupportActionBar(toolbar);
    }

    private void setUpTabLayout() {
        // Add Tab
        gamesChatTabLayout.addTab(gamesChatTabLayout.newTab().setText("games"));
        gamesChatTabLayout.addTab(gamesChatTabLayout.newTab().setText("call & play"));
    }

    private void setUpViewPager(ViewPager viewPager) {
        final ViewPageAdapter viewPagerAdapter = new ViewPageAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFrag(new GamesFragment(), "games");
        ChatFragment chatFragment = new ChatFragment();
        if (intent != null) {
            MyLog.d(LOG_TAG, "bundle set");
            Bundle bundle = new Bundle();
            String userId = intent.getStringExtra("user_id");
            if (userId != null) {
                bundle.putString("user_id", intent.getStringExtra("user_id"));
            }
            if (requestBundle != null) {
                bundle.putBundle("requestBundle", requestBundle);
            }
            chatFragment.setArguments(bundle);
        }
        viewPagerAdapter.addFrag(chatFragment, "chat");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.requestTransparentRegion(viewPager);
        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
            int currentPosition = 0;
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                MyLog.d(LOG_TAG, "onPageScrolled called: " + position + " " + positionOffset + " " + positionOffsetPixels );
                FragmentChangeListener fragmentShown = (FragmentChangeListener) viewPagerAdapter.getItem(1);
                fragmentShown.onScrollFragment(position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                MyLog.d(LOG_TAG, "onPageSelected called");
                FragmentChangeListener fragmentShown = (FragmentChangeListener) viewPagerAdapter.getItem(position);
                fragmentShown.onShowFragment();

                FragmentChangeListener fragmentHidden = (FragmentChangeListener) viewPagerAdapter.getItem(currentPosition);
                fragmentHidden.onHideFragment();

                currentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                MyLog.d(LOG_TAG, "onPageScrollStateChanged called" + state);
            }
        };
        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    private void setUpTabItems() {
        //set view for first tab
        LinearLayout tabFirstItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_layout_item, null);
        TextView textView = (TextView) tabFirstItem.findViewById(R.id.tab_item_text);
        textView.setText("games");
        textView.setTextColor(Color.parseColor("#F2830A"));

        ImageView imageView = (ImageView) tabFirstItem.findViewById(R.id.tab_item_image);
        imageView.setImageResource(R.drawable.games_icon);
        gamesChatTabLayout.getTabAt(0).setCustomView(tabFirstItem);

        //set view for second tab
        LinearLayout tabSecondItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_layout_item, null);
        TextView secondTextView = (TextView) tabSecondItem.findViewById(R.id.tab_item_text);
        secondTextView.setText("call & play");
        //tabSecondItem.setBackgroundResource(R.drawable.second_tab_background);
        secondTextView.setTextColor(Color.parseColor("#2290D3"));
        ImageView secondImageView = (ImageView) tabSecondItem.findViewById(R.id.tab_item_image);
        secondImageView.setImageResource(R.drawable.video_icon);
        gamesChatTabLayout.getTabAt(1).setCustomView(tabSecondItem);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //   Disabled since not must - Check Duo
        if (hasFocus) {
            ViewUtils.showWindowNavigation(getWindow());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MyLog.i(LOG_TAG, "activity onActivityResult focus 1:" + this.hasWindowFocus() + " req:" + requestCode + " result" + resultCode);
        if (resultCode == ChatFragment.INCOMING_CALL_CODE) {
            MyLog.i(LOG_TAG, "Switch to chat tab");
            viewPager.setCurrentItem(1);
        }
    }

    private void refreshFirebaseToken() {
        MyLog.i(LOG_TAG, "Refreshing firebase token");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            MyLog.w(LOG_TAG, "Unable to authenticate firebase");
            return;
        }
        final String firebaseUid = SharedPreferenceUtility.getFirebaseUid(this);
        final String facebookUid = SharedPreferenceUtility.getFacebookUserId(this);
        user.getToken(false /* forceRefresh */)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> tokenTask) {
                        String firebaseIdToken = tokenTask.getResult().getToken();
                        if (firebaseIdToken != null) {
                            FirebaseTokenUtils.saveAllDeviceRegistrationToken(firebaseUid,
                                    facebookUid, GamesChatActivity.this);
                        } else {
                            MyLog.e(LOG_TAG, "Firebase returned null token ");
                        }
                    }
                });
    }

    public static Intent getNewIntentGameChatActivity(Context fromActivityContext) {
        Intent intent = new Intent(fromActivityContext, GamesChatActivity.class);
        intent.setFlags(Intent.FLAG_FROM_BACKGROUND
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /*package local */void launchGameActivity(String gameUrl, boolean isPortrait,
                                              boolean isMultiplayer) {
        Intent gameIntent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MyLog.i(LOG_TAG, "Launching in default browser for below Lollipop.");
            gameIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gameUrl));
        } else {
            gameIntent = new Intent(this, GameWebViewActivity.class);
            gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_URL, gameUrl);
            gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_IS_MULTIPLAYER, isMultiplayer);
            gameIntent.putExtra(GameWebViewActivity.EXTRA_GAME_ORIENTATION, isPortrait);
        }
        startActivity(gameIntent);
    }
}
