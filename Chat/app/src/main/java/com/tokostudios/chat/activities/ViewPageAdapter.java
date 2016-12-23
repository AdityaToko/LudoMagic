package com.tokostudios.chat.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPageAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> tabLayoutList = new ArrayList<>();
        private final List<String> tabLayoutTitleList = new ArrayList<>();

    public ViewPageAdapter(FragmentManager fm) {
        super(fm);
    }


    @Override
        public Fragment getItem(int position) {
            return tabLayoutList.get(position);
        }

        @Override
        public int getCount() {
            return tabLayoutList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            tabLayoutList.add(fragment);
            tabLayoutTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabLayoutTitleList.get(position);
        }
}
