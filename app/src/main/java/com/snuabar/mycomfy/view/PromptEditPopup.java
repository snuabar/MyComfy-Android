package com.snuabar.mycomfy.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.chip.Chip;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.databinding.LayoutPromptEditPopupWindowBinding;
import com.snuabar.mycomfy.main.data.prompt.PromptManager;

import java.util.List;
import java.util.Stack;

public class PromptEditPopup extends PopupWindow {

    private final LayoutPromptEditPopupWindowBinding binding;
    private final OnPromptChangeListener onPromptChangeListener;
    private final Stack<String> undoList;

    public interface OnPromptChangeListener {
        void onPromptChange(String prompt);
    }

    public PromptEditPopup(Context context, OnPromptChangeListener onPromptChangeListener) {
        super(context);
        undoList = new Stack<>();
        this.onPromptChangeListener = onPromptChangeListener;
        binding = LayoutPromptEditPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // 设置对话框样式
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(getContentView().getResources().getDisplayMetrics().heightPixels / 2);
        setFocusable(true);
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.prompt_edit_popup_bg));
        setElevation(8);

        binding.promptEditText.addTextChangedListener(textWatcher);
        binding.switchPromptsPopup.setChecked(binding.promptEditText.getShowSuggestions());
        binding.switchPromptsPopup.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.promptEditText.setShowSuggestions(isChecked));
        binding.switchTranslateZH.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.promptEditText.translateToEN();
            } else {
                binding.promptEditText.translateNone();
            }
        });
        PromptManager.Companion.getInstance().getAllCategories().forEach(c -> addChip(c.getName(), c.getDisplayName()));
    }

    public void setText(String text) {
        binding.switchTranslateZH.setChecked(false);
        undoList.clear();
        binding.promptEditText.setText(text);
        binding.promptEditText.requestFocus();
        binding.promptEditText.setSelection(text.length());
    }

    private void addChip(String name, String displayName) {
        Chip chip = new Chip(getContentView().getContext());
        chip.setBackground(null);
        chip.setText(displayName);
        chip.setTag(name);
        binding.chipGroup.addView(chip);
        ViewGroup.LayoutParams layoutParams = chip.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        chip.setLayoutParams(layoutParams);
        chip.setOnClickListener(this::onClipsClick);
    }

    private void onClipsClick(View v) {
        String name = v.getTag().toString();
        List<String> prompts = PromptManager.Companion.getInstance().getKeywordsByCategory(name);
        binding.promptEditText.showPrompts(v, prompts);
    }

    private void onClick(View v) {
        if (undoList.size() > 1) {
            String previousPrompt = undoList.pop();
            Log.e("!@#", previousPrompt);
//                binding.promptEditText.setText(previousPrompt);
//                Editable editable = binding.promptEditText.getEditableText();
//                editable.clear();
//                editable.append(previousPrompt);
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            undoList.push(s.toString());
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (onPromptChangeListener != null) {
                onPromptChangeListener.onPromptChange(s.toString());
            }
        }
    };

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
