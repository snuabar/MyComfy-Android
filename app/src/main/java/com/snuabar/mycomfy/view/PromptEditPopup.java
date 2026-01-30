package com.snuabar.mycomfy.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.internal.TextWatcherAdapter;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.databinding.LayoutPromptEditPopupWindowBinding;

public class PromptEditPopup extends PopupWindow {

    private LayoutPromptEditPopupWindowBinding binding;

    public PromptEditPopup(Context context, TextWatcherAdapter watcherAdapter) {
        super(context);
        this.binding = LayoutPromptEditPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // 设置对话框样式
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(getContentView().getResources().getDisplayMetrics().heightPixels / 2);
        setFocusable(true);
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.prompt_edit_popup_bg));
        setElevation(8);

        binding.promptEditText.addTextChangedListener(watcherAdapter);
    }

    public void setText(String text) {
        binding.promptEditText.setText(text);
    }


//    @Override
//    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
//        // 手动测量和布局
//        getContentView().measure(
//                View.MeasureSpec.makeMeasureSpec(
//                        View.MeasureSpec.EXACTLY,
//                        View.MeasureSpec.EXACTLY
//                ),
//                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        );
//        getContentView().layout(
//                0, 0,
//                getContentView().getMeasuredWidth(),
//                getContentView().getMeasuredHeight()
//        );
//        super.showAsDropDown(anchor, 0, -anchor.getHeight() - getContentView().getMeasuredHeight(), Gravity.TOP | Gravity.START);
//    }

}
