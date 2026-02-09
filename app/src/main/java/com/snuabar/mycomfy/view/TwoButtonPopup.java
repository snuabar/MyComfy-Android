package com.snuabar.mycomfy.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.ColorRes;
import androidx.core.content.res.ResourcesCompat;

import com.snuabar.mycomfy.databinding.LayoutPopupTwoButtonBinding;
import com.snuabar.mycomfy.utils.ViewUtils;

public class TwoButtonPopup extends GeneralPopup {

    public enum Edge {Start, Top, End, Bottom}

    private final LayoutPopupTwoButtonBinding binding;
    private OnButtonsClickListener listener;

    public TwoButtonPopup(Context context) {
        super(context);
        binding = LayoutPopupTwoButtonBinding.inflate(LayoutInflater.from(context));
        binding.button1.setOnClickListener(this::onClick);
        binding.button2.setOnClickListener(this::onClick);

        setContentView(binding.getRoot());
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public TwoButtonPopup setListener(OnButtonsClickListener listener) {
        this.listener = listener;
        return this;
    }

    public TwoButtonPopup setHeightWrapContent() {
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        return this;
    }

    public TwoButtonPopup setHeightMathParent() {
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        return this;
    }

    public TwoButtonPopup setWidthWrapContent() {
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        return this;
    }

    public TwoButtonPopup setWidthMathParent() {
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        return this;
    }

    public TwoButtonPopup setButton1TextColor(@ColorRes int id) {
        binding.button1.setTextColor(ResourcesCompat.getColor(getContentView().getResources(), id, null));
        return this;
    }

    public TwoButtonPopup setButton2TextColor(@ColorRes int id) {
        binding.button2.setTextColor(ResourcesCompat.getColor(getContentView().getResources(), id, null));
        return this;
    }

    public TwoButtonPopup switchButtons() {
        binding.layoutTwoButtons.removeView(binding.button1);
        binding.layoutTwoButtons.removeView(binding.button2);
        binding.layoutTwoButtons.addView(binding.button2);
        binding.layoutTwoButtons.addView(binding.button1);
        return this;
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

    private void onClick(View v) {
        boolean dismiss = true;
        if (listener != null) {
            if (v == binding.button2) {
                dismiss = listener.onClick(v, 1);
            } else {
                dismiss = listener.onClick(v, 0);
            }
        }

        if (dismiss) {
            dismiss();
        }
    }

    public interface OnButtonsClickListener {
        /**
         * Called when button is pressed.
         *
         * @param view   This button.
         * @param button Button index. Which will be 0 or 1.
         * @return Return {@code false} to block this {@code Popup} to be dismissed.
         */
        boolean onClick(View view, int button);
    }
}
