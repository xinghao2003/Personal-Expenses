package com.example.expenses;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FragmentHandler extends FragmentStateAdapter {

    public FragmentHandler(FragmentManager supportFragmentManager, Lifecycle lifecycle) {
        super(supportFragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new MainFragment();
        } else {
            return new HistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
