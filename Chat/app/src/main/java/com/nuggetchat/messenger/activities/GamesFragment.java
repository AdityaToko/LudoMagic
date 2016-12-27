package com.nuggetchat.messenger.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.nuggetchat.messenger.R;

public class GamesFragment extends Fragment {
    GridView gridView;
    String[] gamesName = {
            "Google",
            "Github",
            "Instagram",
            "Facebook",
    } ;
    int[] gamesImages = {
            R.drawable.nuggeticon,
            R.drawable.game,
            R.drawable.chat_icon,
            R.drawable.game,
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_games_layout, container, false);
        CustomGridAdapter customeGridAdapter = new CustomGridAdapter(getActivity(), gamesName, gamesImages);
        gridView = (GridView)view.findViewById(R.id.grid_view);
        gridView.setAdapter(customeGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(getActivity(), "You Clicked at " + gamesName[position], Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }
}
