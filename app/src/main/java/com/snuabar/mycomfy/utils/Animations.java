package com.snuabar.mycomfy.utils;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.snuabar.mycomfy.R;

public class Animations {
    /**
     * 控件闪烁动画 - 透明度变化
     * @param view 需要闪烁的控件
     * @param duration 动画持续时间（毫秒）
     * @param repeatCount 重复次数（-1表示无限循环）
     */
    public static void startBlinkAnimation(View view, long duration, int repeatCount, float... alphas) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", alphas);
        animator.setDuration(duration);
        animator.setRepeatCount(repeatCount);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        // 保存动画引用以便停止
        view.setTag(R.id.blink_animation_tag, animator);
    }

    /**
     * 停止闪烁动画
     */
    public static void stopBlinkAnimation(View view, float alpha) {
        ObjectAnimator animator = (ObjectAnimator) view.getTag(R.id.blink_animation_tag);
        if (animator != null) {
            animator.cancel();
            view.setAlpha(alpha); // 恢复透明度
            view.setTag(R.id.blink_animation_tag, null);
        }
    }

}
