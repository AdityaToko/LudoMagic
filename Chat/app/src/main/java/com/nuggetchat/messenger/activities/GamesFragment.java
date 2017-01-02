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

public class GamesFragment extends Fragment {
    private static final String LOG_TAG = GamesFragment.class.getSimpleName();
    public static final String EXTRA_GAME_URL = GameWebViewActivity.class.getName() + ".game_url";
    private GridView gridView;
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
        gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(customeGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(getActivity(), "You Clicked at " + gamesName.get(position),
                        Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "the games url, " + gamesUrl.get(position));
                Intent gameIntent = new Intent(getActivity(), GameWebViewActivity.class);
                gameIntent.putExtra(EXTRA_GAME_URL, gamesUrl.get(position));
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
                Log.i(LOG_TAG, "datasnapshot, " + dataSnapshot.getKey());
                GamesData gamesData = dataSnapshot.getValue(GamesData.class);
                gamesName.add(gamesData.getTitle());
                gamesImages.add(gamesData.getFeaturedImage());
                gamesUrl.add(gamesData.getUrl());
                GamesItem gamesItem = new GamesItem(dataSnapshot.getKey(), gamesData.getTitle(),
                        gamesData.getFeaturedImage(), gamesData.getUrl());
                gamesItemList.add(gamesItem);
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
