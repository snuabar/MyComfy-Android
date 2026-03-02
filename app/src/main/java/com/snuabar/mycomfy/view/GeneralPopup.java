package com.snuabar.mycomfy.view;

import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.utils.ViewUtils;

public class GeneralPopup extends PopupWindow {

    public enum Edge {Start, Top, End, Bottom}

    public GeneralPopup(Context context) {
        super(context);
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.popup_bg));
        setOutsideTouchable(true);
        setElevation(8);
        setFocusable(true);
    }

    public void show(View anchor, Edge edge) {
        ViewUtils.measure(getContentView());

        int xOff = 0, yOff = 0;

        if (edge == Edge.Start) {
            xOff = -getContentView().getMeasuredWidth();
            yOff = (int) -(anchor.getHeight() / 2.f + getContentView().getMeasuredHeight() / 2.f);
        } else if (edge == Edge.Top) {
            xOff = (int) -(anchor.getWidth() / 2.f + getContentView().getMeasuredWidth() / 2.f);
            yOff = -(anchor.getHeight() + getContentView().getMeasuredHeight());
        } else if (edge == Edge.End) {
            xOff = -anchor.getWidth();
            yOff = (int) -(anchor.getHeight() / 2.f + getContentView().getMeasuredHeight() / 2.f);
        } else if (edge == Edge.Bottom) {
            xOff = (int) -(anchor.getWidth() / 2.f + getContentView().getMeasuredWidth() / 2.f);
        }

        showAsDropDown(anchor, xOff, yOff);
    }

}
