package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nuggetchat.lib.Conf;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.datamodel.GamesData;

import java.util.ArrayList;

import static com.nuggetchat.messenger.activities.GameWebViewActivity.EXTRA_GAME_ORIENTATION;
import static com.nuggetchat.messenger.activities.GameWebViewActivity.EXTRA_GAME_URL;

public class GamesFragment extends Fragment {
    private static final String LOG_TAG = GamesFragment.class.getSimpleName();
    private ArrayList<String> gamesName;
    private ArrayList<String> gamesImages;
    private ArrayList<String> gamesUrl;
    private ArrayList<GamesItem> gamesItemList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_games_layout, container, false);
        gamesName = new ArrayList<>();
        gamesImages = new ArrayList<>();
        gamesItemList = new ArrayList<>();
        gamesUrl = new ArrayList<>();

        fetchDataForGames();

        CustomGridAdapter customeGridAdapter = new CustomGridAdapter(getActivity(), gamesName, gamesImages);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(customeGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(getActivity(), "You Clicked at " + gamesName.get(position),
                        Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "the games url, " + gamesUrl.get(position));
                Intent gameIntent = new Intent(getActivity(), GameWebViewActivity.class);
                gameIntent.putExtra(EXTRA_GAME_URL, gamesUrl.get(position));
                Log.i(LOG_TAG, "the games isPortrait, " + gamesItemList.get(position).getPortrait());
                gameIntent.putExtra(EXTRA_GAME_ORIENTATION, gamesItemList.get(position).getPortrait());
                startActivity(gameIntent);
            }
        });
        return view;
    }

    private void fetchDataForGames() {
        String firebaseUri = Conf.firebaseGamesURI();
        Log.i(LOG_TAG, "Fetching Games Stream : , " + firebaseUri);

        DatabaseReference firebaseRef = FirebaseDatabase.getInstance()
                .getReferenceFromUrl(firebaseUri);

        if (firebaseRef == null) {
            Log.e(LOG_TAG, "Unable to get database reference.");
            return;
        }

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GamesData gamesData = dataSnapshot.getValue(GamesData.class);
                gamesName.add(gamesData.getTitle());
                gamesImages.add(gamesData.getFeaturedImage());
                gamesUrl.add(gamesData.getUrl());
                GamesItem gamesItem = new GamesItem(dataSnapshot.getKey(), gamesData.getTitle(),
                        gamesData.getFeaturedImage(), gamesData.getUrl(), gamesData.getPortrait());
                gamesItemList.add(gamesItem);
                Log.i(LOG_TAG, "Game " + gamesData.getDataId() + " isPortrait " + gamesData.getPortrait());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
