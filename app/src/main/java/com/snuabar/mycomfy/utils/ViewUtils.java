package com.snuabar.mycomfy.utils;

import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

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

    /**
     * 设置布局中所有控件的可见性，指定ID的控件不受影响
     * @param rootView 根布局
     * @param visibility 要设置的可见性 (View.VISIBLE, View.INVISIBLE, View.GONE)
     * @param excludedViews 不受影响的控件数组
     */
    public static void setViewVisibility(ViewGroup rootView, int visibility, View... excludedViews) {
        // 将排除的ID转换为Set，便于快速查找
        Set<Integer> excludeIds = new HashSet<>();
        for (View v : excludedViews) {
            excludeIds.add(v.getId());
        }

        // 遍历所有子视图
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);

            // 如果当前视图的ID不在排除列表中，则设置可见性
            if (!excludeIds.contains(child.getId())) {
                child.setVisibility(visibility);
            }

            // 如果子视图是ViewGroup，递归处理
            if (child instanceof ViewGroup) {
                setViewVisibility((ViewGroup) child, visibility, excludedViews);
            }
        }
    }
}
