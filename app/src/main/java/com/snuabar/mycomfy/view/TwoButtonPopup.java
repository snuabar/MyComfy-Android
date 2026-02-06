package com.snuabar.mycomfy.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.snuabar.mycomfy.databinding.LayoutPopupTwoButtonBinding;

public class TwoButtonPopup extends GeneralPopup {

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
