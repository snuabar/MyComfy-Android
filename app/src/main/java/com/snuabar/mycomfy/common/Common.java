package com.snuabar.mycomfy.common;

import android.util.DisplayMetrics;
import android.view.Window;

import androidx.annotation.NonNull;

public final class Common {

    /**
     * Gets the width for a dialog by computing the window bounds and insets.
     *
     * @param window Window of a dialog.
     * @return width for a dialog.
     */
    public static int getDialogWidthFromDisplay(@NonNull final Window window) {
        int width, height;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        //Use the minimum length
        return Math.min(width, height);
    }
}
