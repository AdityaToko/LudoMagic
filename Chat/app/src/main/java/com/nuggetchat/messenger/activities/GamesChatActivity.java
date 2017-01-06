package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.chat.ChatService;
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
        startService(new Intent(this, ChatService.class));
        setContentView(R.layout.games_chat_activity);
        ButterKnife.bind(this);

        intent = getIntent();
        requestBundle = intent.getExtras();
        if (requestBundle != null && requestBundle.getString("from") != null) {
            Log.d(LOG_TAG, requestBundle.getString("from") + "");
        }

        setUpToolbar();

        setUpTabLayout();

        setUpViewPager(viewPager);

        gamesChatTabLayout.setupWithViewPager(viewPager);

        refreshFirebaseToken();

        setUpTabItems();

        if (shouldShowChatTab()) {
            viewPager.setCurrentItem(1);
            tabView = (LinearLayout) gamesChatTabLayout.getTabAt(1).getCustomView();
            tabView.setBackgroundResource(R.drawable.second_tab_background);
        } else {
            viewPager.setCurrentItem(0);
            tabView = (LinearLayout) gamesChatTabLayout.getTabAt(0).getCustomView();
            tabView.setBackgroundResource(R.drawable.first_tab_background);
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
                    imageView.setImageResource(R.drawable.chat_icon);
                    tabView.setBackgroundResource(R.drawable.second_tab_background);
                    textView.setTextColor(Color.parseColor("#2290D3"));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                gamesChatTabLayout.getTabAt(tab.getPosition()).getCustomView()
                        .setBackgroundColor(Color.parseColor("#FEFCFF"));
                int position = tab.getPosition();
                LinearLayout tabView = (LinearLayout) gamesChatTabLayout.getTabAt(position).getCustomView();
                TextView textView = (TextView) tabView.findViewById(R.id.tab_item_text);
                ImageView imageView = (ImageView) tabView.findViewById(R.id.tab_item_image);
                if (position == 0) {
                    imageView.setImageResource(R.drawable.games_icon);
                    tabView.setBackgroundColor(Color.parseColor("#F7F3E2"));
                    textView.setTextColor(Color.parseColor("#1cb1be"));
                } else {
                    imageView.setImageResource(R.drawable.chat_icon);
                    tabView.setBackgroundColor(Color.parseColor("#F7F3E2"));
                    textView.setTextColor(Color.parseColor("#F9B21B"));
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private boolean shouldShowChatTab() {
        return intent.getStringExtra("user_id") != null
                || (requestBundle != null
                        && "pre_call_handshake".equals(requestBundle.getString("type")));
    }

    private void setUpToolbar() {
        String userName = SharedPreferenceUtility.getFacebookUserName(this);
        Log.i(LOG_TAG, "the username, " + userName);
        String profilePicUrl = getProfilePicUrl(SharedPreferenceUtility.getFacebookUserId(this));
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
        gamesChatTabLayout.addTab(gamesChatTabLayout.newTab().setText("chat"));
    }

    private void setUpViewPager(ViewPager viewPager) {
        ViewPageAdapter viewPagerAdapter = new ViewPageAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFrag(new GamesFragment(), "games");
        ChatFragment chatFragment = new ChatFragment();
        if (intent != null) {
            Log.d(LOG_TAG, "bundle set");
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
        secondTextView.setText("chat");
        //tabSecondItem.setBackgroundResource(R.drawable.second_tab_background);
        secondTextView.setTextColor(Color.parseColor("#2290D3"));
        ImageView secondImageView = (ImageView) tabSecondItem.findViewById(R.id.tab_item_image);
        secondImageView.setImageResource(R.drawable.chat_icon);
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
        Log.d(LOG_TAG, "activity onActivityResult");
    }

    private String getProfilePicUrl(String facebookUserId) {
        return "https://graph.facebook.com/" + facebookUserId + "/picture?width=200&height=150";
    }

    private void refreshFirebaseToken() {
        Log.i(LOG_TAG, "Refreshing firebase token");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(LOG_TAG, "Unable to authenticate firebase");
            return;
        }
        user.getToken(false /* forceRefresh */)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> tokenTask) {
                        String firebaseIdToken = tokenTask.getResult().getToken();
                        if (firebaseIdToken != null) {
                            SharedPreferenceUtility.setFirebaseIdToken(firebaseIdToken,
                                    GamesChatActivity.this);
                        } else {
                            Log.e(LOG_TAG, "Firebase returned null token ");
                        }
                    }
                });

    }
}
