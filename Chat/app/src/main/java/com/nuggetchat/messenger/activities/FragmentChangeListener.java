package com.nuggetchat.messenger.activities;

public interface FragmentChangeListener {
    void onShowFragment();

    void onHideFragment();

    void onScrollFragment(int position, int offset);
}
