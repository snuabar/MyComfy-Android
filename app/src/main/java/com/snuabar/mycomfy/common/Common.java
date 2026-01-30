package com.snuabar.mycomfy.common;

import android.util.DisplayMetrics;
import android.view.Window;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    /**
     * 根据UTC时间戳格式化显示时间
     * @param timestamp UTC时间戳（毫秒）
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Calendar now = Calendar.getInstance();
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        Calendar yesterdayStart = (Calendar) todayStart.clone();
        yesterdayStart.add(Calendar.DAY_OF_MONTH, -1);

        if (calendar.after(todayStart)) {
            // 今天
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            return "今天 " + sdf.format(date);
        } else if (calendar.after(yesterdayStart)) {
            // 昨天
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            return "昨天 " + sdf.format(date);
        } else {
            // 更久以前
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            return sdf.format(date);
        }
    }
}
