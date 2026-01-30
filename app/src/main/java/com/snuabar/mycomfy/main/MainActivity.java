package com.snuabar.mycomfy.main;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.internal.EdgeToEdgeUtils;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.setting.Settings;
import com.snuabar.mycomfy.setting.SettingsActivity;
import com.snuabar.mycomfy.utils.FilePicker;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.init(this);
        RetrofitClient.init(this);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mViewModel.setFilePicker(new FilePicker(this));
        setContentView(R.layout.activity_main);
        EdgeToEdgeUtils.applyEdgeToEdge(getWindow(), true);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        mViewModel.getDeletionModeLiveData().observe(this, aBoolean -> invalidateOptionsMenu());

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean deletionMode = Boolean.TRUE.equals(mViewModel.getDeletionModeLiveData().getValue());
        menu.findItem(R.id.action_settings).setVisible(!deletionMode);
        menu.findItem(R.id.action_delete).setVisible(deletionMode);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // 点击设置按钮，跳转到设置页面
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_delete) {
            mViewModel.changeDeletionHasPressed(true);
            mViewModel.changeDeletionHasPressed(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (Boolean.TRUE.equals(mViewModel.getDeletionModeLiveData().getValue())) {
                mViewModel.changeDeletionMode(false);
                return;
            }
            setEnabled(false);
            getOnBackPressedDispatcher().onBackPressed();
        }
    };
}