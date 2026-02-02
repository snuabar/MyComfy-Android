package com.snuabar.mycomfy.main;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.snuabar.mycomfy.databinding.FragmentMainBinding;

import java.util.Objects;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getName();

    private FragmentMainBinding binding;

    private MainViewModel mViewModel;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.viewPager2.setAdapter(new FragmentAdapter(getChildFragmentManager(), getLifecycle()));
        binding.viewPager2.setCurrentItem(0);
        binding.viewPager2.setOffscreenPageLimit(1);
        binding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback);
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        mViewModel.getSelectedTabLiveData().observe(getViewLifecycleOwner(), tab -> {
            Objects.requireNonNull(binding.tabLayout.getTabAt(tab)).select();
            binding.viewPager2.setCurrentItem(tab, true);
        });
    }

    private final ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            mViewModel.changeSelectedTab(binding.viewPager2.getCurrentItem());
        }
    };

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewModel.changeSelectedTab(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private static class FragmentAdapter extends FragmentStateAdapter {

        private final Fragment[] fragments;

        public FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
            fragments = new Fragment[]{HomeFragment.newInstance(), HistoryFragment.newInstance()};
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments[position];
        }

        @Override
        public int getItemCount() {
            return fragments.length;
        }
    }
}