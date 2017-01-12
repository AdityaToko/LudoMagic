package com.nuggetchat.messenger.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
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
import com.nuggetchat.messenger.utils.FirebaseAnalyticsConstants;
import com.nuggetchat.messenger.utils.SharedPreferenceUtility;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

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

        numberOfFriends = SharedPreferenceUtility.getNumberOfFriends(this.getContext());
        numberLocked = TOTAL_NUMBER_LOCKED - UNLOCK_INCENTIVE * numberOfFriends;
        fetchDataForGames(this.getContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (numberOfFriends < SharedPreferenceUtility.getNumberOfFriends(this.getContext())) {
            int newNumberOfFriends = SharedPreferenceUtility.getNumberOfFriends(this.getContext());
            int newNumberLocked = TOTAL_NUMBER_LOCKED - UNLOCK_INCENTIVE * newNumberOfFriends;
            int toBeUnlocked = newNumberLocked - numberLocked;
            processUnlockGames(toBeUnlocked, newNumberOfFriends, newNumberLocked);
        }
    }

    private void processUnlockGames(int toBeUnlocked, final int newNumberOfFriends, final int newNumberLocked) {
        new AlertDialog.Builder(this.getContext())
                .setTitle(R.string.unlock_games_dialog_title)
                .setMessage(R.string.unlock_games_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with unlock
                        unlockGames(newNumberLocked);
                        numberOfFriends = newNumberOfFriends;
                        numberLocked = newNumberLocked;
                    }
                })
                .setIcon(R.drawable.games_icon)
                .show();
    }

    private void unlockGames(int newNumberLocked) {
        for (int i = gamesItemList.size() - numberLocked; i <= gamesItemList.size() - numberLocked; i++) {
            Log.d("GAMESFRAGMENT", ">>>>UNLOCKING: " + String.valueOf(i) + "  " + gamesItemList.get(i).getGamesName());
            gamesItemList.get(i).setLocked(false);
            gamesItemList.get(i).setNewlyUnlocked(true);
        }
        setUpGridView(this.getContext());
    }

    private void fetchDataForGames(final Context context) {
        String firebaseMultiPlayerGamesUri = Conf.firebaseMultiPlayerGamesUri();
        Log.i(LOG_TAG, "Fetching MultiPlayer Games Stream : , " + firebaseMultiPlayerGamesUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseMultiPlayerGamesUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot itemDataSnapshot : dataSnapshot.getChildren()) {
                    String id = itemDataSnapshot.getKey();
                    multiplayerIDList.add(0, id);
                    Log.d(LOG_TAG, ">>>multiplayer id: " + id);
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
        Log.i(LOG_TAG, "Fetching Games Stream : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        for (int i = 0; i < multiplayerIDList.size(); i++) {
            Log.d(LOG_TAG, ">>multi ids: " + multiplayerIDList.get(i));
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
                                Log.d("GAMEFRAGMENT", ">>>>MULTI GAMES: " + multiplayerGamesItemList.size() + " " + "false");
                            } else {
                                gamesItemList.add(0, gamesItem);
                                Log.d("GAMEFRAGMENT", ">>>>SOLO GAMES: " + gamesItemList.size() + " " + "false");
                            }

                        } else {
                            GamesItem gamesItem = new GamesItem(gamesData.getDataId(), gamesData.getTitle(),
                                    gamesData.getFeaturedImage(), gamesData.getUrl(), gamesData.getPortrait(), true, false, gamesData.getValueScore());

                            if (multiplayerIDList.contains(gamesData.getDataId())) {
                                multiplayerGamesItemList.add(0, gamesItem);
                                Log.d("GAMEFRAGMENT", ">>>>MULTI GAMES: " + multiplayerGamesItemList.size() + " " + "false");
                            } else {
                                gamesItemList.add(0, gamesItem);
                                Log.d("GAMEFRAGMENT", ">>>>SOLO GAMES: " + gamesItemList.size() + " " + "true");
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
        Log.i(LOG_TAG, "grid view set, " + gamesItemList);
        CustomGridAdapter customGridAdapter = new CustomGridAdapter(getActivity(), gamesItemList);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(customGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                if (gamesItemList.get(position).getLocked()) {

                    new AlertDialog.Builder(context)
                            .setTitle(R.string.add_friends_dialog_title)
                            .setMessage(R.string.add_friends_dialog_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with add friends
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setPackage("com.facebook.orca");
                                    intent.setType("text/plain");
                                    intent.putExtra(Intent.EXTRA_TEXT, "Hey! Found this app where we can play multiplayer games while voice-calling! Install it so we can play: http://bit.ly/2iTz71P");

                                    try {
                                        startActivity(intent);
                                    } catch (android.content.ActivityNotFoundException ex) {
                                        Toast.makeText(context, "You do not have Facebook Messenger installed", Toast.LENGTH_LONG).show();
                                    }
                                    NuggetInjector.getInstance().logEvent(FirebaseAnalyticsConstants.ADD_FACEBOOK_FRIENDS_BUTTON_CLICKED,
                                            null /* bundle */);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with unlock

                                }
                            })
                            .setIcon(R.drawable.child_pic_one_icon)
                            .show();

                } else {
                    Toast.makeText(getActivity(), "Starting " + gamesItemList.get(position).getGamesName(),
                            Toast.LENGTH_SHORT).show();
                    nuggetInjector.logEvent(FirebaseAnalyticsConstants.SOLO_GAMES_BUTTON_CLICKED,
                            null /* bundle */);
                    Log.i(LOG_TAG, "the games url, " + gamesItemList.get(position).getGamesUrl());
                    ((GamesChatActivity) getActivity()).launchGameActivity(
                            gamesItemList.get(position).getGamesUrl(),
                            gamesItemList.get(position).getPortrait(),
                            false /*isMultiplayer*/);
                }
            }
        });
    }

    @Override
    public void onShowFragment() {
        Log.d(LOG_TAG, "onShowFragment: Games Fragment shown");
    }

    @Override
    public void onHideFragment() {
        Log.d(LOG_TAG, "onShowFragment: Games Fragment hidden");
    }

    @Override
    public void onScrollFragment(int position, int offset) {

    }
}

