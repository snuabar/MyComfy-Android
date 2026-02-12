package com.snuabar.mycomfy.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.Window;

import androidx.annotation.NonNull;

import com.snuabar.mycomfy.BuildConfig;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
     *
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
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return "今天 " + sdf.format(date);
        } else if (calendar.after(yesterdayStart)) {
            // 昨天
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return "昨天 " + sdf.format(date);
        } else {
            // 更久以前
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            return sdf.format(date);
        }
    }

    public static int calcScale(int size, double factor) {
        return (int) (Math.round(Math.round(size * factor) / 8.0) * 8.0);
    }

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    /**
     * 格式化文件大小，自动选择合适的单位
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
     */
    public static String formatFileSize(long size) {
        if (size < 0) {
            return "0 B";
        }

        DecimalFormat df = new DecimalFormat("#.##");

        if (size < KB) {
            return size + " B";
        } else if (size < MB) {
            return df.format((double) size / KB) + " KB";
        } else if (size < GB) {
            return df.format((double) size / MB) + " MB";
        } else if (size < TB) {
            return df.format((double) size / GB) + " GB";
        } else {
            return df.format((double) size / TB) + " TB";
        }
    }

    /**
     * Get a flag that defines whether the system has a navigation bar.
     *
     * @return Show of not show.
     */
    public static boolean isSystemHasStatusBar(Context context) {
        if (context == null) {
            return false;
        }
        Resources res = context.getResources();
        @SuppressLint("DiscouragedApi")
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId != 0) {
            String strNavBarOverride = null;
            try {
                @SuppressLint("PrivateApi")
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                strNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable ignored) {
            }

            boolean hasNav;
            if ("1".equals(strNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(strNavBarOverride)) {
                hasNav = true;
            } else {
                hasNav = res.getBoolean(resourceId);
            }
            return hasNav;
        }
        return !ViewConfiguration.get(context).hasPermanentMenuKey();
    }


    /**
     * Gets the defined height of the system navigation bar.
     *
     * @return Height.
     */
    public static int getNavigationBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        int result = 0;
        if (isSystemHasStatusBar(context)) {
            Resources res = context.getResources();
            @SuppressLint({"DiscouragedApi", "InternalInsetResource"})
            int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * Gets the defined height of system status bar.
     *
     * @return Height.
     */
    public static int getStatusBarHeight(Context context) {
        if (context == null) {
            return 0;
        }

        @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 获取图像浏览的放大倍率
     *
     * @param context 上下文
     * @param bmpWidth 要显示的位图宽度
     * @param bmpHeight 要显示的位图高度
     * @return 长度为2的数组，0：最大倍率，1：中间倍率
     */
    public static float[] getPhotoViewScales(Context context, float bmpWidth, float bmpHeight) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        float defWScale = (float) metrics.widthPixels / bmpWidth;
        float defHScale = (float) metrics.heightPixels / bmpHeight;
        float usedScale = Math.min(defHScale, defWScale);
        float midScale;
        if (defHScale > defWScale) {
            if (isLandscape) {
                midScale = Math.min(metrics.widthPixels, metrics.heightPixels) / (bmpHeight * usedScale);
            } else {
                midScale = Math.max(metrics.widthPixels, metrics.heightPixels) / (bmpHeight * usedScale);
            }
        } else {
            if (isLandscape) {
                midScale = Math.max(metrics.widthPixels, metrics.heightPixels) / (bmpHeight * usedScale);
            } else {
                midScale = Math.min(metrics.widthPixels, metrics.heightPixels) / (bmpHeight * usedScale);
            }
        }
        float maxScale = midScale * 3;
        if (midScale < 1.0f) {
            midScale = Math.max(maxScale / 2, midScale);
            if (midScale < 1.0) {
                midScale = 1.75f;
                maxScale = 3.f;
            }
        }

        return new float[]{maxScale, midScale};
    }

    public static String correctPackageLikeStringsForDebug(String str) {
        if (BuildConfig.DEBUG) {
            if (str.contains(BuildConfig.defaultPackageName) &&
                    !str.contains(BuildConfig.defaultPackageName + ".debug")) {
                str = str.replaceAll(BuildConfig.defaultPackageName, BuildConfig.defaultPackageName + ".debug");
            }
        }
        return str;
    }
}
