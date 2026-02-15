package com.snuabar.mycomfy.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.databinding.LayoutPromptOnlyPopupWindowBinding;
import com.snuabar.mycomfy.utils.Animations;
import com.snuabar.mycomfy.utils.ImageUtils;

import java.io.File;

public class PromptOnlyPopup extends GeneralPopup {

    private final static String TAG = PromptOnlyPopup.class.getName();

    private final LayoutPromptOnlyPopupWindowBinding binding;
    private OnDismissListener onDismissListener;
    private Parameters parameters;
    private int clickedButton = 0;

    public PromptOnlyPopup(Context context) {
        super(context);
        binding = LayoutPromptOnlyPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        setupListeners();
    }

    private void setupListeners() {
        binding.switchPromptsPopup.setChecked(binding.promptEditText.getShowSuggestions());
        binding.switchPromptsPopup.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.promptEditText.setShowSuggestions(isChecked));
        binding.chipGroupTranslation.setOnCheckedStateChangeListener((chipGroup, list) -> {
            if (list.contains(R.id.chipEnZh)) {
                binding.promptEditText.translatePromptToZH();
            } else if (list.contains(R.id.chipZhEn)) {
                binding.promptEditText.translatePromptToEN();
            } else {
                binding.promptEditText.translateNone();
            }
        });
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnSubmit.setOnClickListener(v -> {
            clickedButton = 1;
            dismiss();
        });
    }

    private void displayPicture() {
        File file = parameters.getImageFiles()[0];
        File thumbnail = null;
        if (file != null) {
            thumbnail = ImageUtils.getThumbnailFileInCacheDir(getContentView().getContext(), file);
            if (!thumbnail.exists()) {
                float width = getContentView().getContext().getResources().getDimension(R.dimen.thumbnail_width);
                float height = getContentView().getContext().getResources().getDimension(R.dimen.thumbnail_height);
                thumbnail = ImageUtils.createAndSaveThumbnail(file, thumbnail, width, height);
            }
        }
        Bitmap bmp;
        if (thumbnail != null && thumbnail.exists()) {
            bmp = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
        } else {
            bmp = null;
        }
        binding.imageView1.setImageBitmap(bmp);
    }

    private String getPrompt() {
        Editable editable = binding.promptEditText.getText();
        return editable == null ? "" : editable.toString().trim();
    }

    private String getSeed() {
        Editable editable = binding.etSeed.getText();
        return editable == null ? "" : editable.toString().trim();
    }

    private int getStep() {
        Editable editable = binding.etStep.getText();
        String str = editable == null ? "" : editable.toString().trim();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return parameters.getStep();
        }
    }

    private double getCfg() {
        Editable editable = binding.etCFG.getText();
        String str = editable == null ? "" : editable.toString().trim();
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return parameters.getCfg();
        }
    }

    private int getSeconds() {
        Editable editable = binding.etSeconds.getText();
        String str = editable == null ? "" : editable.toString().trim();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return parameters.getSeconds();
        }
    }

    public void show(View anchor, Edge edge, Parameters parameters) {
        super.show(anchor, edge);
        this.parameters = new Parameters(parameters);

        binding.promptEditText.setText(this.parameters.getPrompt());
        binding.etSeed.setText(this.parameters.getSeed());
        binding.etStep.setText(String.valueOf(this.parameters.getStep()));
        binding.etCFG.setText(String.valueOf(this.parameters.getCfg()));
        binding.etSeconds.setText(String.valueOf(this.parameters.getSeconds()));
        displayPicture();
    }

    @Override
    public void dismiss() {
        parameters.setPrompt(getPrompt());
        parameters.setSeed(getSeed());
        parameters.setStep(getStep());
        parameters.setCfg(getCfg());
        parameters.setSeconds(getSeconds());
        int button = clickedButton;
        clickedButton = 0;
        if (button == 1 && getPrompt().trim().isEmpty()) {
            Animations.startBlinkAnimation(binding.viewPromptAlert, 400, 1, 0f, 1f, 0f);
            return;
        }
        super.dismiss();
        if (onDismissListener != null) {
            onDismissListener.onDismiss(button == 1, parameters);
        }
    }

    public void setOnDismissListener(OnDismissListener callback) {
        this.onDismissListener = callback;
    }

    public interface OnDismissListener {
        void onDismiss(boolean submit, Parameters parameters);
    }
}
