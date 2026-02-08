package com.snuabar.mycomfy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

public class PressOnOffSwitch extends androidx.appcompat.widget.AppCompatImageButton {

    private boolean isOn = false;
    private OnStateChangeListener onStateChangeListener;

    public PressOnOffSwitch(Context context) {
        super(context);
    }

    public PressOnOffSwitch(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PressOnOffSwitch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 按下逻辑
                setOn(true);
                break;
            case MotionEvent.ACTION_UP:
                // 抬起逻辑
                performClick();
            case MotionEvent.ACTION_CANCEL:
                // 取消逻辑
                setOn(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    public void setOn(boolean on) {
        if (isOn != on) {
            isOn = on;
            if (onStateChangeListener != null) {
                onStateChangeListener.onStateChange(isOn);
            }
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    public interface OnStateChangeListener {
        void onStateChange(boolean isOn);
    }
}
