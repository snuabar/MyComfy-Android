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

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.databinding.FragmentMainBinding;
import com.snuabar.mycomfy.main.data.MainViewModel;

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
        Objects.requireNonNull(binding.tabLayout.getTabAt(0)).view.setOnClickListener(v -> mViewModel.notifyTabClicked(0));
        Objects.requireNonNull(binding.tabLayout.getTabAt(1)).view.setOnClickListener(v -> mViewModel.notifyTabClicked(1));

        mViewModel.getSelectedTabLiveData().observe(getViewLifecycleOwner(), tab -> {
            Objects.requireNonNull(binding.tabLayout.getTabAt(tab)).select();
            binding.viewPager2.setCurrentItem(tab, true);
        });
        mViewModel.getSearchingModeLiveData().observe(getViewLifecycleOwner(), this::setSearingMode);
    }

    private final TextWatcher searchBoxTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mViewModel.searchData(s.toString());
        }
    };

    private void setSearingMode(boolean searchingMode) {
        binding.etSearchBox.setEnabled(searchingMode);
        if (searchingMode) {
            binding.etSearchBox.addTextChangedListener(searchBoxTextWatcher);
            new Handler().postDelayed(() -> binding.etSearchBox.requestFocus(), 200);
        } else {
            binding.etSearchBox.removeTextChangedListener(searchBoxTextWatcher);
            binding.etSearchBox.setText(null);
        }
        binding.btnCloseSearchBox.setOnClickListener(v -> mViewModel.setSearchingMode(false));
        binding.layoutSearchBox.getLayoutParams().height = searchingMode ? (int) getResources().getDimension(R.dimen.search_box_height) : 0;
        binding.layoutSearchBox.requestLayout();
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