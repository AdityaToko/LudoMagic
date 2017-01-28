package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.lib.model.UserInfo;
import com.nuggetchat.messenger.FragmentChangeListener;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.chat.ChatService;
import com.nuggetchat.messenger.chat.UpdateInterface;
import com.nuggetchat.messenger.datamodel.CurrentLeader;
import com.nuggetchat.messenger.datamodel.LastLeader;
import com.nuggetchat.messenger.datamodel.PrizeWinner;
import com.nuggetchat.messenger.utils.FirebaseTokenUtils;
import com.nuggetchat.messenger.utils.FirebaseUtils;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;
import com.nuggetchat.messenger.utils.ViewUtils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GamesChatActivity extends AppCompatActivity implements UpdateInterface {
    private static final String LOG_TAG = GamesChatActivity.class.getSimpleName();
    private static final long INCENTIVE_START_TS = 1485508492;
    private static final long DELTA_PRIZE_TIME = 300;
    private static final int PERCENTAGE_MINIMUM1 = 40;
    private static final int CURRENT_LEADER_MINIMUM_1 = 0;
    private static final int CURRENT_LEADER_MINIMUM_2 = 2;
    private static final int RANDOM_RANGE = 1;

    private Long lastPrizeTS;
    private Long nextPrizeTS;
    private long currentPrizeCounter;
    private long startPrizeCounter;
    private int currentGiftScore;
    private int currentLeaderScore;
    private CountDownTimer counter = null;

    @BindView(R.id.toolbar)
    /* package-local */ Toolbar toolbar;

    @BindView(R.id.tabLayout)
    /* package-local */ TabLayout gamesChatTabLayout;

    @BindView(R.id.pager)
    /* package-local */ ViewPager viewPager;

    @BindView(R.id.gift_button)
    /* package-local */ ImageView giftButton;

    @BindView(R.id.image)
    /* package-local */ ImageView image;

    @BindView(R.id.next_prize_time)
    /* package-local */ TextView nextPrizeTime;

    @BindView(R.id.your_score)
    /* package-local */ TextView yourScoreText;

    @BindView(R.id.current_leader_score)
    /* package-local */ TextView leaderScoreText;

    private LinearLayout tabView;
    private TextView textView;
    private ImageView imageView;
    private Intent intent;
    private Bundle requestBundle;
    private Handler mainHandler;

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
        mainHandler = new Handler(getMainLooper());
        setUpToolbar(this);

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
        if (checkFirstRun()) {
            createIncentiveInfoDialog(this);
            getCurrentLeaderAndUpdate();
        }
        Log.d(LOG_TAG,"Calling Incentive Actions 1");
        incentiveActions(this);

    }

    @Override
    protected void onStop() {
        if (counter != null) {
            counter.cancel();
        }
        super.onStop();
    }

    private void incentiveActions(Context context) {
        Log.d(LOG_TAG,">>> Here0");
        currentLeaderScore = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getInt("currentLeaderScore", 0);
        currentGiftScore = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getInt("currentGiftScore", 0);
        setCurrentGiftScore(currentGiftScore);
        setLeaderScore(currentLeaderScore);
        long currentTS = System.currentTimeMillis() / 1000;
        long deltaPrizeTime = DELTA_PRIZE_TIME;
        lastPrizeTS = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getLong("lastPrizeTS", -1l);
        nextPrizeTS = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getLong("nextPrizeTS", -1l);

        Log.d(LOG_TAG,">>> Here1 lastPrizeTS: " + lastPrizeTS + " nextPrizeTS: " + nextPrizeTS);
        if (lastPrizeTS.equals(-1l) || nextPrizeTS.equals(-1l)) {
            Log.d(LOG_TAG,">>> Here2");

            long startTS = INCENTIVE_START_TS;
            long deltaCurrentStart = currentTS - startTS;
            int periods = 0;
            if (deltaPrizeTime != 0l) {
                Log.d(LOG_TAG,">>> Here3");
                periods = (int) (deltaCurrentStart / deltaPrizeTime);
                lastPrizeTS = startTS + deltaPrizeTime * ((long) periods);
                nextPrizeTS = startTS + deltaPrizeTime * ((long) (periods + 1));
            }

            Log.d(LOG_TAG,">>> currentTS: " + currentTS);
            Log.d(LOG_TAG,">>> startTS: " + startTS);
            Log.d(LOG_TAG,">>> deltaPrizeTime: " + deltaPrizeTime);
            Log.d(LOG_TAG,">>> deltaCurrentStart: " + deltaCurrentStart);
            Log.d(LOG_TAG,">>> periods: " + periods);

            SharedPreferences prefs = getSharedPreferences("PREFERENCE", MODE_PRIVATE);
            if (prefs != null) {
                Log.d(LOG_TAG,">>> Here4");
                prefs.edit()
                    .putLong("lastPrizeTS", lastPrizeTS)
                    .putLong("nextPrizeTS", nextPrizeTS)
                    .apply();
            }

        }


        Log.d(LOG_TAG,">>> lastPrizeTS: " + lastPrizeTS);
        Log.d(LOG_TAG,">>> nextPrizeTS: " + nextPrizeTS);
        Log.d(LOG_TAG,">>> current-last: " + (currentTS-lastPrizeTS));

        if ((currentTS - lastPrizeTS) > (deltaPrizeTime-500)) {
            Log.d(LOG_TAG,">>> Here5");
            lastPrizeTS = nextPrizeTS;
            nextPrizeTS = nextPrizeTS + deltaPrizeTime;
            SharedPreferences prefs = getSharedPreferences("PREFERENCE", MODE_PRIVATE);
            if (prefs != null) {
                Log.d(LOG_TAG,">>> Here6");
                prefs.edit()
                        .putLong("lastPrizeTS", lastPrizeTS)
                        .putLong("nextPrizeTS", nextPrizeTS)
                        .apply();
            }
            Log.d(LOG_TAG,">>> Here7");
            currentGiftScore = 0;
            setCurrentGiftScore(currentGiftScore);
            prizeWinActions(context);
        }
        Log.d(LOG_TAG,">>> Before starting counter - lastPrizeTS: " + lastPrizeTS);
        Log.d(LOG_TAG,">>> Before starting counter - nextPrizeTS: " + nextPrizeTS);
        startCounter(nextPrizeTS - currentTS, context);

    }

    private void startCounter(long start, final Context context) {
        Log.d(LOG_TAG, ">>>Starting counter: " + start);
        String nextPrizeTimeText;
        int seconds = (int) (start);
        int minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        int hours = minutes / 60;
        minutes = minutes - hours * 60;
        int days = hours / 24;
        hours = hours - days * 24;

        nextPrizeTimeText = days + "d:" + String.format("%02d", hours) + "h:" + String.format("%02d", minutes)
                + "m:" + String.format("%02d", seconds) + "s";
        setNextPrizeTime(nextPrizeTimeText);

        counter = new CountDownTimer(start * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds - minutes * 60;
                int hours = minutes / 60;
                minutes = minutes - hours * 60;
                int days = hours / 24;
                seconds = seconds % 60;
                hours = hours - days * 24;

                String nextPrizeTimeText = days + "d:" + String.format("%02d", hours) + "h:" + String.format("%02d", minutes)
                        + "m:" + String.format("%02d", seconds) + "s";
                setNextPrizeTime(nextPrizeTimeText);

                if((millisUntilFinished <= 2100) && (millisUntilFinished >= 1100)) {
                    Log.d(LOG_TAG,"Calling Incentive Actions 2");
                    incentiveActions(context);
                }
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    private void setNextPrizeTime(final String nextPrizeTimeText) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                nextPrizeTime.setText(String.valueOf(nextPrizeTimeText));
                nextPrizeTime.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setCurrentGiftScore(final int currentGiftScore) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                yourScoreText.setText(String.valueOf(currentGiftScore));
                yourScoreText.setVisibility(View.VISIBLE);
            }
        });
    }

    public void updateScore(String myUserID, String targetUserID) {
        //Do only if played with new user
        SharedPreferences pref = getSharedPreferences("PREFERENCE", MODE_PRIVATE);
        Set<String> someStringSet = pref.getStringSet("playedWith", new HashSet<String>());
        Log.d(LOG_TAG, "=>>> Played with list: " + someStringSet);

        if (!someStringSet.contains(targetUserID)) {

            someStringSet.add(targetUserID);
            Log.d(LOG_TAG, "=>>> Updated played with list: " + someStringSet);

            pref.edit().putStringSet("playedWith", someStringSet).apply();

            currentGiftScore = currentGiftScore + 1;
            setCurrentGiftScore(currentGiftScore);

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putInt("currentGiftScore", currentGiftScore)
                    .apply();

            getCurrentLeaderAndUpdate();
        }
    }

    public void setLeaderScore(final int leaderScore) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                leaderScoreText.setText(String.valueOf(leaderScore));
                leaderScoreText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void getCurrentLeaderAndUpdate() {
        final String myID = SharedPreferenceUtility.getFacebookUserId(this);
        String fbaseCurrentLeaderUri = Conf.firebaseCurrentLeaderUri();
        DatabaseReference firebaseDataRef = FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseCurrentLeaderUri);
        Log.d(LOG_TAG, "=>>> Getting Current Leader & Updating " + firebaseDataRef);

        firebaseDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                Log.d(LOG_TAG, "=>>> Here0");
                CurrentLeader currentLeader = null;
                try {
                    currentLeader = dataSnapshot.getValue(CurrentLeader.class);
                } catch (Exception e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
                Log.d(LOG_TAG, "=>>> dataSnapshot: " + dataSnapshot.toString());
                int currentGlobalLeaderScore = currentLeader.getScore();
                int globalLeaderMinimum1 = currentLeader.getMinimum1();
                int globalLeaderMinimum2 = currentLeader.getMinimum2();
                int compareMinimum;

                long leaderTimeLeft = currentLeader.getScoreTime();

                long currentTS = System.currentTimeMillis() / 1000;
                long timeLeft = nextPrizeTS - currentTS;

                Log.d(LOG_TAG, "=>>> Info: currentGlobal: " + currentGlobalLeaderScore
                        + " globalMin1: " + globalLeaderMinimum1
                        + " globalMin2: " + globalLeaderMinimum2
                        + " leaderTimeLeft: " + leaderTimeLeft
                        + " currentTS: " + currentTS
                        + " timeLeft: " + timeLeft
                );

                if (currentGlobalLeaderScore > currentGiftScore) {
                    Log.d(LOG_TAG, "==> Here1");
                    setLeaderScore(currentGlobalLeaderScore);
                    getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .edit()
                            .putInt("currentLeaderScore", currentGlobalLeaderScore)
                            .apply();

                } else if (currentGlobalLeaderScore <= currentGiftScore) {
                    Log.d(LOG_TAG, "==> Here2");
                    if (timeLeft >= (DELTA_PRIZE_TIME * PERCENTAGE_MINIMUM1 / 100)) {
                        Log.d(LOG_TAG, "==> Here3");
                        compareMinimum = globalLeaderMinimum1;
                    } else {
                        Log.d(LOG_TAG, "==> Here4");
                        compareMinimum = globalLeaderMinimum2;
                    }

                    if (currentGlobalLeaderScore < compareMinimum) {
                        Log.d(LOG_TAG, "==> Here5");
                        Random random = new Random();
                        int add = random.nextInt(RANDOM_RANGE);
                        currentGlobalLeaderScore = compareMinimum + add;
                        setLeaderScore(currentGlobalLeaderScore);
                        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                .edit()
                                .putInt("currentLeaderScore", currentGlobalLeaderScore)
                                .apply();
                        CurrentLeader update = new CurrentLeader("none",
                                currentLeader.getMinName(), currentLeader.getMinimum1(), currentLeader.getMinimum2(),
                                currentGlobalLeaderScore, timeLeft);
                        updateFbaseLeaderScore(update);
                    } else {
                        Log.d(LOG_TAG, "==> Here6");
                        if (currentGlobalLeaderScore == currentGiftScore) {
                            Log.d(LOG_TAG, "==> Here7");
                            if (timeLeft > leaderTimeLeft) {
                                Log.d(LOG_TAG, "==> Here8");
                                currentGlobalLeaderScore = currentGiftScore;
                                setLeaderScore(currentGlobalLeaderScore);
                                CurrentLeader update = new CurrentLeader(myID,
                                        currentLeader.getMinName(), currentLeader.getMinimum1(), currentLeader.getMinimum2(),
                                        currentGlobalLeaderScore, timeLeft);
                                updateFbaseLeaderScore(update);
                            } else {
                                Log.d(LOG_TAG, "==> Here9");
                                setLeaderScore(currentGlobalLeaderScore);
                            }
                            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                    .edit()
                                    .putInt("currentLeaderScore", currentGlobalLeaderScore)
                                    .apply();
                        } else if (currentGlobalLeaderScore < currentGiftScore) {
                            Log.d(LOG_TAG, "==> Here10");
                            currentGlobalLeaderScore = currentGiftScore;
                            setLeaderScore(currentGlobalLeaderScore);
                            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                    .edit()
                                    .putInt("currentLeaderScore", currentGlobalLeaderScore)
                                    .apply();
                            CurrentLeader update = new CurrentLeader(myID,
                                    currentLeader.getMinName(), currentLeader.getMinimum1(), currentLeader.getMinimum2(),
                                    currentGlobalLeaderScore, timeLeft);
                            updateFbaseLeaderScore(update);
                        }
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(LOG_TAG, "Error in fetching data.", databaseError.toException());
            }
        });

    }

    private void updateFbaseLeaderScore(CurrentLeader update) {
        Log.d(LOG_TAG, "=>>> Updating Fbase Leader Score: " + update.getId() + " score: " + update.getScore() + " timeLeft: " + update.getScoreTime());
        if (update.getId() == "none") {
            updateFbaseMinimumLeaderScore(update);
        } else {
            findFbaseIDUpdateLeaderScore(update);
        }
    }


    private void updateFbaseMinimumLeaderScore(CurrentLeader update) {
        String fbaseCurrentLeaderUri = Conf.firebaseCurrentLeaderUri();
        Log.d(LOG_TAG, ">>>Writing to: " + fbaseCurrentLeaderUri);

        DatabaseReference histRef =
                FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseCurrentLeaderUri);
        FirebaseUtils.fireBaseSetValue(histRef, update, "user-id:" + update.getId());
    }


    private void findFbaseIDUpdateLeaderScore(final CurrentLeader update) {
        String fbaseFbToFireidUri = Conf.firebaseFbToFireidUri(update.getId());
        DatabaseReference firebaseDataRef = FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseFbToFireidUri);
        Log.d(LOG_TAG, "=>>> Getting Fireid & Updating " + firebaseDataRef);

        firebaseDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String firebaseID = dataSnapshot.getValue(String.class);
                Log.d(LOG_TAG, "=>>> Writing to Firebase: " + firebaseID);
                update.setId(firebaseID);
                updateFbaseMinimumLeaderScore(update);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void prizeWinActions(final Context context) {
//        createYouWonDialog(context);
//        createOtherWonDialog(context, "Testy Tester");

        String fbaseLastLeaderUri = Conf.firebaseLastLeaderUri();
        DatabaseReference firebaseDataRef = FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseLastLeaderUri);
        Log.d(LOG_TAG, "*>>> Getting Last Leader & Updating " + firebaseDataRef);

        firebaseDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                Log.d(LOG_TAG, "*>>> Here0");
                LastLeader lastLeader = null;
                try {
                    Log.d(LOG_TAG,"*>>> Last leader: " + dataSnapshot.toString());
                    lastLeader = dataSnapshot.getValue(LastLeader.class);
                    Log.d(LOG_TAG,"*>>> Last leader: " + lastLeader.toString());
                    Log.d(LOG_TAG,"*>>> Last leader id: " + lastLeader.getId());
                    if ("none".equals(lastLeader.getId())) {
                        if ((lastLeader.getName() != null) && (lastLeader.getName().trim() != "")) {
                            Log.d(LOG_TAG, "*>>> Here1");
                            createOtherWonDialog(context, lastLeader.getName().toUpperCase());
                        }
                    } else {
                        Log.d(LOG_TAG, "*>>> Here2");
                        getFbaseIdWinActions(context, lastLeader.getId());
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, e.getMessage());
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(LOG_TAG, "Error in fetching data.", databaseError.toException());
            }
        });

    }

    private void getFbaseIdWinActions(final Context context, String id) {
        final String myID = SharedPreferenceUtility.getFacebookUserId(this);

        String fbaseUsersUri = Conf.firebaseUsersUri();
        DatabaseReference firebaseDataRef = FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseUsersUri);
        Log.d(LOG_TAG, "=>>> Getting Fbase ID for Win Actions " + firebaseDataRef);

        firebaseDataRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                UserInfo lastLeader = null;
                try {
                    lastLeader = dataSnapshot.getValue(UserInfo.class);
                    Log.d(LOG_TAG,"*>>> Last leader Fbase: " + lastLeader.toString());
                    if (lastLeader.getFacebookId().equals(myID)) {
                        giftButton.setBackgroundResource(R.drawable.got_gift);
                        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                .edit()
                                .putBoolean("toClaimGift", true)
                                .apply();
                        createYouWonDialog(context);
                    } else {
                        createOtherWonDialog(context, lastLeader.getName());
                    }

                } catch (Exception e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(LOG_TAG, "Error in fetching data.", databaseError.toException());
            }
        });
    }


    private void createOtherWonDialog(final Context context, final String winnerNameText) {
        Log.d(LOG_TAG, "==>>> Creating Other Won Dialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Play with friends", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewPager.setCurrentItem(1);
            }
        })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        final AlertDialog dialog = builder.create();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (inflater != null) {
            View dialogLayout = inflater.inflate(R.layout.incentive_otherswon_layout, null);
            TextView winnerName = (TextView) dialogLayout.findViewById(R.id.winner_name);
            winnerName.setText(winnerNameText);
            dialog.setView(dialogLayout);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            dialog.show();
        }

    }


    private void createYouWonDialog(final Context context) {
        Log.d(LOG_TAG, "==>>> Creating YouWon Dialog");

//        YouWonDialog.Builder builder = new YouWonDialog.Builder(context);
//        final YouWonDialog dialog = (YouWonDialog) builder.create();

        LayoutInflater inflater = LayoutInflater.from(context);
        final YouWonDialog dialog = new YouWonDialog(context, inflater);

        if (inflater != null) {
            Log.d(LOG_TAG, "==>>> Expanding YouWon View");

            try {
                dialog.show();
            } catch (Exception e) {
                Log.d(LOG_TAG, "==>>>" + e.getMessage());
            }
        } else {
            Log.d(LOG_TAG, "==>>> Inflater null");
        }
    }


    private void writeWinnerDetailsToFbase(String email, String address) {
        String fbaseWinnersUri = Conf.firebaseWinnersUri();
        Log.d(LOG_TAG, ">>>Writing to: " + fbaseWinnersUri);
        String dateClaimed = Utils.getCurrentDate();
        PrizeWinner winner = new PrizeWinner(dateClaimed, email, address);

        DatabaseReference histRef =
                FirebaseDatabase.getInstance().getReferenceFromUrl(fbaseWinnersUri);
        FirebaseUtils.fireBaseSetValue(histRef.push(), winner, "user-id:" + winner.getDate());
    }


    private void createIncentiveInfoDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Play with friends", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewPager.setCurrentItem(1);
            }
        })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        final AlertDialog dialog = builder.create();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (inflater != null) {
            View dialogLayout = inflater.inflate(R.layout.incentive_info_layout, null);
            dialog.setView(dialogLayout);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            dialog.show();
        }
    }

    public boolean checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun) {
            // Place your dialog code here to display the dialog

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
            return true;
        }
        return false;
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

    private void setUpToolbar(final Context context) {
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

        giftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean toClaimGift = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("toClaimGift", true);
                if(toClaimGift) {
                    createYouWonDialog(context);
                } else {
                    createIncentiveInfoDialog(context);
                }
            }
        });

        setSupportActionBar(toolbar);
    }

    private void setUpTabLayout() {
        // Add Tab
        gamesChatTabLayout.addTab(gamesChatTabLayout.newTab().setText(R.string.games_tab_heading));
        gamesChatTabLayout.addTab(gamesChatTabLayout.newTab().setText(R.string.call_tab_heading));
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
                MyLog.d(LOG_TAG, "onPageScrolled called: " + position + " " + positionOffset + " " + positionOffsetPixels);
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
        textView.setText(R.string.games_tab_heading);
        textView.setTextColor(Color.parseColor("#F2830A"));

        ImageView imageView = (ImageView) tabFirstItem.findViewById(R.id.tab_item_image);
        imageView.setImageResource(R.drawable.games_icon);
        gamesChatTabLayout.getTabAt(0).setCustomView(tabFirstItem);

        //set view for second tab
        LinearLayout tabSecondItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_layout_item, null);
        TextView secondTextView = (TextView) tabSecondItem.findViewById(R.id.tab_item_text);
        secondTextView.setText(R.string.call_tab_heading);
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

    @Override
    public void updateReceiverScore(String from, String to) {
        Log.d(LOG_TAG, ">>>Update Reciever Score");
        updateScore(from, to);
    }


    public class YouWonDialog extends android.app.AlertDialog {
        EditText inputEmail;
        EditText inputAddress;
        TextView inputEmailHint;
        TextView inputAddressHint;

        public YouWonDialog(Context context, LayoutInflater inflater) {
            super(context);
            if (inflater != null) {
                View dialogLayout = inflater.inflate(R.layout.incentive_youwon_layout, null);
                Log.d(LOG_TAG, "==>>> Have I crashed? 1");

                setView(dialogLayout);
                requestWindowFeature(Window.FEATURE_NO_TITLE);

                inputEmail = (EditText) dialogLayout.findViewById(R.id.input_email);
                inputAddress = (EditText) dialogLayout.findViewById(R.id.input_address);
                inputEmailHint = (TextView) dialogLayout.findViewById(R.id.input_email_hint);
                inputAddressHint = (TextView) dialogLayout.findViewById(R.id.input_address_hint);

                inputEmailHint.setVisibility(View.INVISIBLE);
                inputAddressHint.setVisibility(View.INVISIBLE);

                setButton(android.app.AlertDialog.BUTTON_POSITIVE, "Ok", (new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // this will never be called
                    }
                }));
            }
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String inputEmailText = inputEmail.getText().toString();
                    String inputAddressText = inputAddress.getText().toString();

                    if (inputEmailText.isEmpty()) {
                        // do something
                        inputEmailHint.setText("Email can't be empty");
                        inputEmailHint.setVisibility(View.VISIBLE);
                    } else if (inputAddressText.isEmpty()) {
                        inputAddressHint.setText("Address can't be empty");
                        inputAddressHint.setVisibility(View.VISIBLE);
                    } else if ((!inputEmailText.contains("@")) || (!inputEmailText.contains(".com"))) {
                        inputEmailHint.setText("Please enter a valid email");
                        inputEmailHint.setVisibility(View.VISIBLE);
                    } else {
                        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                .edit()
                                .putBoolean("toClaimGift", false)
                                .apply();
                        giftButton.setBackgroundResource(R.drawable.gift);
                        writeWinnerDetailsToFbase(inputEmailText, inputAddressText);
                        dismiss();
                    }
                }
            });
        }
    }
}
