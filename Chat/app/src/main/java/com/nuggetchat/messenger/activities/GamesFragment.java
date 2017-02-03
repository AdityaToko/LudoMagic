package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.messenger.FragmentChangeListener;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.datamodel.GamesData;
import com.nuggetchat.messenger.utils.AnalyticConstants;
import com.nuggetchat.messenger.utils.MyLog;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GamesFragment extends Fragment implements FragmentChangeListener {
    private static final String LOG_TAG = GamesFragment.class.getSimpleName();
    private static final int TOTAL_NUMBER_LOCKED = 20;
    private static final int UNLOCK_INCENTIVE = 2;
    @BindView(R.id.loading_icon)
    ProgressBar loadingIcon;

    private ArrayList<GamesItem> gamesItemList;
    private ArrayList<GamesItem> multiplayerGamesItemList;
    private ArrayList<String> multiplayerIDList;

    private NuggetInjector nuggetInjector;
    private TextFeedbackFragment textFeedbackFragment;
    private View view;
    private int numberOfFriends;
    private int numberLocked;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_games_layout, container, false);
        ButterKnife.bind(this, view);

        gamesItemList = new ArrayList<>();
        multiplayerGamesItemList = new ArrayList<>();
        multiplayerIDList = new ArrayList<>();
        nuggetInjector = NuggetInjector.getInstance();
        textFeedbackFragment = new TextFeedbackFragment();

        numberOfFriends = SharedPreferenceUtility.getNumberOfFriends(this.getContext());
        numberLocked = TOTAL_NUMBER_LOCKED - UNLOCK_INCENTIVE * numberOfFriends;
        fetchDataForGames(this.getContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void fetchDataForGames(final Context context) {
        String firebaseMultiPlayerGamesUri = Conf.firebaseMultiPlayerGamesUri();
        MyLog.i(LOG_TAG, "Fetching MultiPlayer Games Stream : , " + firebaseMultiPlayerGamesUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseMultiPlayerGamesUri);

        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot itemDataSnapshot : dataSnapshot.getChildren()) {
                    String id = itemDataSnapshot.getKey();
                    multiplayerIDList.add(0, id);
                    MyLog.d(LOG_TAG, ">>>multiplayer id: " + id);
                }
                fetchAllGames(context);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void fetchAllGames(final Context context) {
        String firebaseUri = Conf.firebaseGamesUri();
        MyLog.i(LOG_TAG, "Fetching Games Stream : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            MyLog.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        for (int i = 0; i < multiplayerIDList.size(); i++) {
            MyLog.d(LOG_TAG, ">>multi ids: " + multiplayerIDList.get(i));
        }


        firebaseRef.orderByChild("valueScore").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                loadingIcon.setVisibility(View.INVISIBLE);
                for (DataSnapshot itemDataSnapshot : dataSnapshot.getChildren()) {
                    GamesData gamesData = itemDataSnapshot.getValue(GamesData.class);

                    if (gamesData != null) {

                        if (gamesItemList.size() >= numberLocked) {
                            GamesItem gamesItem = new GamesItem(gamesData.getDataId(), gamesData.getTitle(),
                                    gamesData.getFeaturedImage(), gamesData.getUrl(), gamesData.getPortrait(), false, false, gamesData.getValueScore());

                            if (multiplayerIDList.contains(gamesData.getDataId())) {
                                multiplayerGamesItemList.add(0, gamesItem);
                                MyLog.d("GAMEFRAGMENT", ">>>>MULTI GAMES: " + multiplayerGamesItemList.size() + " " + "false");
                            } else {
                                gamesItemList.add(0, gamesItem);
                                MyLog.d("GAMEFRAGMENT", ">>>>SOLO GAMES: " + gamesItemList.size() + " " + "false");
                            }

                        } else {
                            GamesItem gamesItem = new GamesItem(gamesData.getDataId(), gamesData.getTitle(),
                                    gamesData.getFeaturedImage(), gamesData.getUrl(), gamesData.getPortrait(), true, false, gamesData.getValueScore());

                            if (multiplayerIDList.contains(gamesData.getDataId())) {
                                multiplayerGamesItemList.add(0, gamesItem);
                                MyLog.d("GAMEFRAGMENT", ">>>>MULTI GAMES: " + multiplayerGamesItemList.size() + " " + "false");
                            } else {
                                gamesItemList.add(0, gamesItem);
                                MyLog.d("GAMEFRAGMENT", ">>>>SOLO GAMES: " + gamesItemList.size() + " " + "true");
                            }

                        }
                    }
                }

                setUpGridView(context);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void setUpGridView(final Context context) {
        MyLog.i(LOG_TAG, "grid view set, " + gamesItemList);
        CustomGridAdapter customGridAdapter = new CustomGridAdapter(getActivity(), gamesItemList);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(customGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(getActivity(), "Starting " + gamesItemList.get(position).getGamesName(),
                        Toast.LENGTH_SHORT).show();
                nuggetInjector.logEvent(AnalyticConstants.SOLO_GAMES_BUTTON_CLICKED,
                        null /* bundle */);
                nuggetInjector.getMixpanel().track(AnalyticConstants.SOLO_GAMES_BUTTON_CLICKED,
                        gamesItemList.get(position).getGamesName());
                MyLog.i(LOG_TAG, "the games url, " + gamesItemList.get(position).getGamesUrl());
                ((GamesChatActivity) getActivity()).launchGameActivity(
                        gamesItemList.get(position).getGamesUrl(),
                        gamesItemList.get(position).getPortrait(),
                        false /*isMultiplayer*/, "", "");

            }
        });
    }

    @OnClick(R.id.show_text_feedback_button)
    /* package-local */ void showFeedbackFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        textFeedbackFragment.show(fragmentTransaction, "feedback");
    }

    @Override
    public void onShowFragment() {
        MyLog.d(LOG_TAG, "onShowFragment: Games Fragment shown");
    }

    @Override
    public void onHideFragment() {
        MyLog.d(LOG_TAG, "onShowFragment: Games Fragment hidden");
    }

    @Override
    public void onScrollFragment(int position, int offset) {

    }
}

