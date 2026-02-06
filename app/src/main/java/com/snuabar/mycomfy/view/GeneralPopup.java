package com.snuabar.mycomfy.view;

import android.content.Context;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.snuabar.mycomfy.R;

public class GeneralPopup extends PopupWindow {

    public GeneralPopup(Context context) {
        super(context);
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.popup_bg));
        setOutsideTouchable(true);
        setElevation(8);
        setFocusable(true);
    }
}
