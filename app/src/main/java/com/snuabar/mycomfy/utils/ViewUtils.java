package com.snuabar.mycomfy.utils;

import android.view.View;

public final class ViewUtils {
    public static void measure(View view) {
        // 手动测量和布局
        view.measure(
                View.MeasureSpec.makeMeasureSpec(
                        0,
                        View.MeasureSpec.UNSPECIFIED
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        view.layout(
                0, 0,
                view.getMeasuredWidth(),
                view.getMeasuredHeight()
        );
    }
}
