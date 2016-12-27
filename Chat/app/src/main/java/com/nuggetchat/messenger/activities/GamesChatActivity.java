package com.nuggetchat.messenger.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nuggetchat.messenger.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GamesChatActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    /* package-local */ Toolbar toolbar;

    @BindView(R.id.tabLayout)
    /* package-local */ TabLayout gamesChatTabLayout;

    @BindView(R.id.pager)
    /* package-local */ ViewPager viewPager;

    @BindView(R.id.kid_name_text)
    /* package-local */ TextView kidNameText;

    @BindView(R.id.kid_image)
    /* package-local */ ImageView kidImage;

    private TextView tabItem;
    private TextView tabItemText;
    private ImageView tabItemImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.games_chat_activity);
        ButterKnife.bind(this);

        setUpToolbar();

        setUpTabLayout();

        setUpViewPager(viewPager);

        gamesChatTabLayout.setupWithViewPager(viewPager);

        setUpTabItems();

        gamesChatTabLayout.getTabAt(0).select();

        gamesChatTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();

                LinearLayout tabView = (LinearLayout) gamesChatTabLayout.getTabAt(position).getCustomView();
                TextView textView = (TextView) tabView.findViewById(R.id.tab_item_text);
                ImageView imageView = (ImageView) tabView.findViewById(R.id.tab_item_image);
                if (position == 0) {
                    imageView.setImageResource(R.drawable.game);
                    tabView.setBackgroundResource(R.drawable.first_tab_background);
                    textView.setTextColor(Color.parseColor("#FF4081"));
                } else  {
                    imageView.setImageResource(R.drawable.chat_icon);
                    tabView.setBackgroundResource(R.drawable.second_tab_background);
                    textView.setTextColor(Color.parseColor("#FF4081"));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                gamesChatTabLayout.getTabAt(tab.getPosition()).getCustomView().setBackgroundColor(Color.parseColor("#FEFCFF"));
                int position = tab.getPosition();
                LinearLayout tabView = (LinearLayout) gamesChatTabLayout.getTabAt(position).getCustomView();
                TextView textView = (TextView) tabView.findViewById(R.id.tab_item_text);
                ImageView imageView = (ImageView) tabView.findViewById(R.id.tab_item_image);
                if(position == 0) {
                    imageView.setImageResource(R.drawable.game);
                    tabView.setBackgroundColor(Color.parseColor("#BEBEBE"));
                    textView.setTextColor(Color.parseColor("#1cb1be"));
                } else {
                    imageView.setImageResource(R.drawable.chat_icon);
                    tabView.setBackgroundColor(Color.parseColor("#BEBEBE"));
                    textView.setTextColor(Color.parseColor("#F9B21B"));
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void setUpToolbar() {
        kidNameText.setText("Vartika Sharma");
        kidImage.setImageResource(R.drawable.nuggeticon);
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
        viewPagerAdapter.addFrag(new ChatFragmet(), "chat");
        viewPager.setAdapter(viewPagerAdapter);
    }

    private void setUpTabItems() {
        //set view for first tab
        LinearLayout tabFirstItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_layout_item, null);
        TextView textView = (TextView) tabFirstItem.findViewById(R.id.tab_item_text);
        textView.setText("games");

        ImageView imageView = (ImageView) tabFirstItem.findViewById(R.id.tab_item_image);
        imageView.setImageResource(R.drawable.nuggeticon);
        gamesChatTabLayout.getTabAt(0).setCustomView(tabFirstItem);

        //set view for second tab
        LinearLayout tabSecondItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_layout_item, null);
        TextView secondTextView = (TextView) tabSecondItem.findViewById(R.id.tab_item_text);
        secondTextView.setText("chat");
        ImageView secondImageView = (ImageView) tabSecondItem.findViewById(R.id.tab_item_image);
        secondImageView.setImageResource(R.drawable.nuggeticon);
        gamesChatTabLayout.getTabAt(1).setCustomView(tabSecondItem);
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
