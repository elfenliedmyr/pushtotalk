package com.auxilium.auxiliummobilesolutions;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class PageAdapter extends FragmentPagerAdapter {
    private int numTabs;

    public PageAdapter(FragmentManager fm, int numTabs) {
        super(fm);
        this.numTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new FragmentMessengerContactList();
            case 1:
                return new FragmentMessengerChatList();
            default:
                return null;
        }
    }

    @Override
    public int getCount() { return numTabs; }
}
