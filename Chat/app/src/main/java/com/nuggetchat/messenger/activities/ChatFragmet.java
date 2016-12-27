package com.nuggetchat.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nuggetchat.messenger.R;
import com.tokostudios.chat.ChatActivity;

public class ChatFragmet extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        startActivity(intent);
        return inflater.inflate(R.layout.fragment_chat_fragmet, container, false);
    }
}
