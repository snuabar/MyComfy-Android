package com.snuabar.mycomfy.view.dialog;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.common.Common;

/**
 * 各 DialogFragment基类
 */
public class BaseDialogFragment extends DialogFragment {

    protected Configuration mConfiguration;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.change_single_name_data_dialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        mConfiguration = newConfig;
        updateUI();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    /**
     * 更新布局，用于支持横竖屏的切换
     */
    protected void updateUI() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                final int width = (int) (Common.getDialogWidthFromDisplay(window) * 0.8f);
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }
}
