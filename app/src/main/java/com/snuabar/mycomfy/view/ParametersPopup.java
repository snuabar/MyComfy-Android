package com.snuabar.mycomfy.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.databinding.LayoutParametersPopupWindowBinding;
import com.snuabar.mycomfy.databinding.LayoutWorkflowItemBinding;
import com.snuabar.mycomfy.main.data.prompt.PromptManager;
import com.snuabar.mycomfy.setting.Settings;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class ParametersPopup extends GeneralPopup {

    private final static String TAG = ParametersPopup.class.getName();

    private WeakReference<View> mAnchor;
    private final LayoutParametersPopupWindowBinding binding;
    private final Map<String, WorkflowsResponse.Workflow> workflows = new HashMap<>();
    private final List<String> models = new ArrayList<>();
    private WorkflowAdapter workflowAdapter;
    private ArrayAdapter<String> modelAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private JSONObject paramJsonObject = null;
    private String paramKey = null;// 使用加密强度的随机数生成器
    private final SecureRandom secureRandom = new SecureRandom();
    private final Stack<String> undoList;
    private OnSubmitCallback onSubmitCallback;
    private final DataRequirer dataRequirer;
    private PickPicturePopup pickPicturePopup;
    private final PictureInfo[] pictureInfos;

    public ParametersPopup(Context context, DataRequirer dataRequirer) {
        super(context);
        this.dataRequirer = dataRequirer;
        undoList = new Stack<>();
        pictureInfos = new PictureInfo[]{new PictureInfo(), new PictureInfo(), new PictureInfo()};
        binding = LayoutParametersPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        // 设置事件
        setupListeners();
        // 设置工作流控件
        setupAdapters();
        handler.post(this::loadWorkflows);

        PromptManager.Companion.getInstance().getAllCategories().forEach(c -> addChip(c.getName(), c.getDisplayName()));
        loadPrompt();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        // 手动测量和布局
        ViewUtils.measure(getContentView());
        mAnchor = new WeakReference<>(anchor);
        super.showAsDropDown(anchor, 0, -anchor.getHeight() - getContentView().getMeasuredHeight(), Gravity.TOP | Gravity.START);
        binding.btnSwitchWH.setEnabled(false);
        handler.post(this::loadWorkflows);
    }

    @Override
    public void update() {
        if (mAnchor != null && mAnchor.get() != null) {
            // 手动测量和布局
            ViewUtils.measure(getContentView());
            View anchor = mAnchor.get();
            super.update(anchor, 0, -anchor.getHeight() - getContentView().getMeasuredHeight(), -1, -1);
        } else {
            super.update();
        }
    }

    @Override
    public void dismiss() {
        saveValues();
        super.dismiss();
    }

    private void setupListeners() {
        // 更新工作流按扭
        binding.btnLoadWorkflows.setOnClickListener(v -> loadWorkflows());
        // 更新工作流按扭
        binding.btnLoadModels.setOnClickListener(v -> loadModels());
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
        binding.btnSwitchWH.setOnClickListener(v -> switchWidthAndHeight());
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnSubmit.setOnClickListener(v -> {
            dismiss();
            if (onSubmitCallback != null) {
                onSubmitCallback.apply();
                generateSeed();
            }
        });
        binding.imageView1.setOnClickListener(this::onImageViewClick);
        binding.imageView2.setOnClickListener(this::onImageViewClick);
        binding.imageView3.setOnClickListener(this::onImageViewClick);
    }

    private void onImageViewClick(View v) {
        if (pickPicturePopup == null) {
            pickPicturePopup = new PickPicturePopup(getContentView().getContext());
        }
        pickPicturePopup.setListener((view, button) -> {
            int which = 0;
            if (v == binding.imageView2) {
                which = 1;
            } else if (v == binding.imageView3) {
                which = 2;
            }
            if (button == 0) {
                setPictureFile(null, which);
            } else {
                dataRequirer.requirePicture(which, button == 2);
            }
            return true;
        }).showAsDropDown(v);
    }

    private void displayPicture(int which, Bitmap bmp) {
        ImageView imageView = binding.imageView1;
        if (which == 1) {
            imageView = binding.imageView2;
        } else if (which == 2) {
            imageView = binding.imageView3;
        }

        if (bmp != null) {
            imageView.setImageBitmap(bmp);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            imageView.setImageResource(R.drawable.ic_add);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
        }
    }

    public void setPictureFile(File file, int which) {
        if (which < 0 || which > 2) {
            return;
        }

        pictureInfos[which].setFile(file);
        Bitmap bmp = pictureInfos[which].bmp;
        displayPicture(which, bmp);
    }

    public void setOnSubmitCallback(OnSubmitCallback callback) {
        this.onSubmitCallback = callback;
    }

    private void generateSeed() {
        int seedCtl = getSeedCtl();
        if (seedCtl == 3) {
            return;
        }

        long seed = 0;
        if (seedCtl == 0) {
            byte[] bytes = new byte[Long.BYTES];
            secureRandom.nextBytes(bytes);
            // 转换为 long，确保为正数
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (bytes[i] & 0xFF);
            }
            seed = Math.abs(seed);
        } else if (seedCtl == 1) {
            String text = binding.etSeed.getText().toString();
            if (TextUtils.isEmpty(text)) {
                text = "0";
            }
            seed = Long.parseLong(text);
            seed++;
        } else if (seedCtl == 2) {
            String text = binding.etSeed.getText().toString();
            if (TextUtils.isEmpty(text)) {
                text = "0";
            }
            seed = Long.parseLong(text);
            seed--;
        }
        binding.etSeed.setText(String.valueOf(seed));
        saveValues();
    }

    private void setSeedCtl(int ctl) {
        binding.chipSeedRandom.setChecked(ctl == 0);
        binding.chipSeedIncrease.setChecked(ctl == 1);
        binding.chipSeedDecrease.setChecked(ctl == 2);
        binding.chipSeedFixed.setChecked(ctl == 3);
    }

    private int getSeedCtl() {
        if (binding.chipSeedRandom.isChecked()) {
            return 0;
        } else if (binding.chipSeedIncrease.isChecked()) {
            return 1;
        } else if (binding.chipSeedDecrease.isChecked()) {
            return 2;
        }
        return 3;
    }

    public void reload() {
        loadPrompt();
        loadWorkflows();
    }

    private void loadPrompt() {
        String text = Settings.getInstance().getPrompt(getContentView().getContext().getString(R.string.default_prompt));
        binding.chipGroupTranslation.clearCheck();
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
        binding.promptEditText.showPromptsAsDropDown(v, prompts);
    }

    private void switchWidthAndHeight() {
        String widthStr = binding.etWidth.getText().toString();
        String heightStr = binding.etHeight.getText().toString();
        binding.etWidth.setText(heightStr);
        binding.etHeight.setText(widthStr);
    }

    private void setupAdapters() {
        workflowAdapter = new WorkflowAdapter(getContentView().getContext());
        modelAdapter = new ArrayAdapter<>(getContentView().getContext(), R.layout.layout_model_item, R.id.text1);
        binding.spinnerWorkflow.setAdapter(workflowAdapter);
        binding.spinnerModels.setAdapter(modelAdapter);
        binding.spinnerWorkflow.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedWorkflow = binding.spinnerWorkflow.getItemAtPosition(position).toString();
                Settings.getInstance().edit().setWorkflow(selectedWorkflow).apply();
                loadModels();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.spinnerModels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = binding.spinnerModels.getItemAtPosition(position).toString();
                if (!Settings.getInstance().getModelName("").equals(selectedModel)) {
                    saveValues();
                    Settings.getInstance().edit().setModelName(selectedModel).apply();
                    loadValues();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void loadWorkflows() {
        dataRequirer.loadWorkflows();
    }

    public void setWorkflows(Map<String, WorkflowsResponse.Workflow> workflowList) {
        workflows.clear();
        workflows.putAll(workflowList);
        List<String> workflowNames = new ArrayList<>(workflows.keySet());
        workflowAdapter.clear();
        workflowAdapter.addAll(workflowNames);
        String selectedWorkflow = Settings.getInstance().getWorkflow(null);
        int index = workflowNames.indexOf(selectedWorkflow);
        if (index < 0 || index >= workflows.size()) {
            index = 0;
            String workflowName = workflows.isEmpty() ? "" : workflowNames.get(index);
            Settings.getInstance().edit().setWorkflow(workflowName).apply();
        }
        binding.spinnerWorkflow.setSelection(index);

        loadModels();
    }

    private void loadModels() {
        models.clear();
        modelAdapter.clear();
        if (binding.spinnerWorkflow.getSelectedItem() == null) {
            loadValues();
            return;
        }

        String selectedWorkflow = binding.spinnerWorkflow.getSelectedItem().toString();
        WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
        if (workflow == null) {
            loadValues();
            return;
        }

        List<String> modelTypes = workflow.getModelTypes();
        if (modelTypes == null) {
            loadValues();
            return;
        }

        binding.layoutModels.setAlpha(modelTypes.isEmpty() ? 0.3f : 1.0f);
        binding.spinnerModels.setEnabled(!modelTypes.isEmpty());
        binding.btnLoadModels.setEnabled(!modelTypes.isEmpty());

        if (modelTypes.isEmpty()) {
            loadValues();
            return;
        }

        dataRequirer.loadModels(modelTypes);
    }

    public void setModels(List<String> modelNames) {
        String selectedWorkflow = binding.spinnerWorkflow.getSelectedItem().toString();
        WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
        if (workflow == null) {
            loadValues();
            return;
        }

        List<String> modelTypes = workflow.getModelTypes();
        if (modelTypes == null) {
            loadValues();
            return;
        }

        models.clear();
        models.addAll(modelNames);
        if (workflow.getModelKeywords() != null && !workflow.getModelKeywords().isEmpty()) {
            models.removeIf(m -> workflow.getModelKeywords().stream().noneMatch(m::contains));
        }
        if (workflow.getExcludeModelKeywords() != null && !workflow.getExcludeModelKeywords().isEmpty()) {
            models.removeIf(m -> workflow.getExcludeModelKeywords().stream().anyMatch(m::contains));
        }
        modelAdapter.clear();
        modelAdapter.addAll(models);
        String selectedModel = Settings.getInstance().getModelName(null);
        int index = models.indexOf(selectedModel);
        if (index < 0 || index >= models.size()) {
            index = 0;
            String modeName = models.isEmpty() ? "" : models.get(index);
            Settings.getInstance().edit().setModelName(modeName).apply();
        }
        binding.spinnerModels.setSelection(index);
        loadValues();
    }

    private String getParametersSettingKey() {
        String selectedWorkflow = Settings.getInstance().getWorkflow("");
        String selectedModel = Settings.getInstance().getModelName("");
        return selectedWorkflow + "-" + selectedModel;
    }

    private JSONObject getParametersJSON() {
        String key = getParametersSettingKey();
        if (paramJsonObject == null || !key.equals(paramKey)) {
            paramKey = key;
            String jsonString = Settings.getInstance().getString(key, "");
            try {
                paramJsonObject = new JSONObject(jsonString);
            } catch (JSONException e) {
                Log.e(TAG, "getParametersJSON. Failed to load json.", e);
//                paramJsonObject = new JSONObject(Settings.DefaultParamMap);
                paramJsonObject = new JSONObject();
                WorkflowsResponse.DefaultParameters dp;
                String selectedWorkflow = Settings.getInstance().getWorkflow("");
                WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
                if (workflow != null && workflow.getDefaultParameters() != null) {
                    dp = workflow.getDefaultParameters();
                } else {
                    dp = WorkflowsResponse.DefaultParameters.Default;
                }
                try {
                    paramJsonObject.putOpt(Settings.KEY_PARAM_WIDTH, String.valueOf(dp.getWidth()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_HEIGHT, String.valueOf(dp.getHeight()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_SEED, String.valueOf(dp.getSeed()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_STEP, String.valueOf(dp.getStep()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_CFG, String.valueOf(dp.getCfg()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_UPSCALE_FACTOR, String.valueOf(dp.getUpscale_factor()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_SECONDS, String.valueOf(dp.getSeconds()));
                    paramJsonObject.putOpt(Settings.KEY_PARAM_MEGAPIXELS, String.valueOf(dp.getMegapixels()));
                } catch (JSONException e1) {
                    Log.e(TAG, "getParametersJSON. default. Failed to execute putOpt.", e1);
                }
            }
        }
        return paramJsonObject;
    }

    private void loadValues() {
        JSONObject jsonObject = getParametersJSON();
        binding.etWidth.setText(jsonObject.optString(Settings.KEY_PARAM_WIDTH));
        binding.etHeight.setText(jsonObject.optString(Settings.KEY_PARAM_HEIGHT));
        binding.etSeed.setText(jsonObject.optString(Settings.KEY_PARAM_SEED));
        binding.etStep.setText(jsonObject.optString(Settings.KEY_PARAM_STEP));
        binding.etCFG.setText(jsonObject.optString(Settings.KEY_PARAM_CFG));
        binding.etUpscaleFactor.setText(jsonObject.optString(Settings.KEY_PARAM_UPSCALE_FACTOR));
        binding.etSeconds.setText(jsonObject.optString(Settings.KEY_PARAM_SECONDS));
        binding.btnSwitchWH.setEnabled(true);
        binding.etMegapixels.setText(jsonObject.optString(Settings.KEY_PARAM_MEGAPIXELS));

        boolean layoutChanged = false;
        int visibility = binding.layoutSeconds.getVisibility();
        binding.layoutSeconds.setVisibility(isVideoWorkflow() ? View.VISIBLE : View.GONE);
        if (binding.layoutSeconds.getVisibility() != visibility) {
            layoutChanged = true;
        }

        visibility = binding.layoutImageSize.getVisibility();
        binding.layoutImageSize.setVisibility(isWorkflowInputImage() ? View.GONE : View.VISIBLE);
        if (binding.layoutImageSize.getVisibility() != visibility) {
            layoutChanged = true;
        }

        visibility = binding.layoutMegapixels.getVisibility();
        binding.layoutMegapixels.setVisibility(isWorkflowInputImage() ? View.VISIBLE : View.GONE);
        if (binding.layoutMegapixels.getVisibility() != visibility) {
            layoutChanged = true;
        }

        visibility = binding.layoutImages.getVisibility();
        binding.layoutImages.setVisibility(isWorkflowInputImage() ? View.VISIBLE : View.GONE);
        if (binding.layoutImages.getVisibility() != visibility) {
            layoutChanged = true;
        }
        if (binding.layoutImages.getVisibility() == View.VISIBLE) {
            File file1 = null, file2 = null, file3 = null;
            if (jsonObject.has(Settings.KEY_PARAM_IMAGE1)) {
                file1 = new File(jsonObject.optString(Settings.KEY_PARAM_IMAGE1));
            }
            if (jsonObject.has(Settings.KEY_PARAM_IMAGE2)) {
                file2 = new File(jsonObject.optString(Settings.KEY_PARAM_IMAGE2));
            }
            if (jsonObject.has(Settings.KEY_PARAM_IMAGE3)) {
                file3 = new File(jsonObject.optString(Settings.KEY_PARAM_IMAGE3));
            }
            setPictureFile(file1, 0);
            setPictureFile(file2, 1);
            setPictureFile(file3, 2);
        }

        setSeedCtl(jsonObject.optInt(Settings.KEY_PARAM_SEED_CTL, 0));

        if (layoutChanged) {
            update();
        }
    }

    private void saveValues() {
        Settings.getInstance().setPrompt(binding.promptEditText.getText() == null ? "" : binding.promptEditText.getText().toString());
        JSONObject jsonObject = getParametersJSON();
        try {
            jsonObject.putOpt(Settings.KEY_PARAM_WIDTH, binding.etWidth.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_HEIGHT, binding.etHeight.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_SEED, binding.etSeed.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_SEED_CTL, getSeedCtl());
            jsonObject.putOpt(Settings.KEY_PARAM_STEP, binding.etStep.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_CFG, binding.etCFG.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_UPSCALE_FACTOR, binding.etUpscaleFactor.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_SECONDS, binding.etSeconds.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_MEGAPIXELS, binding.etMegapixels.getText().toString());
            if (pictureInfos[0].file == null) {
                jsonObject.remove(Settings.KEY_PARAM_IMAGE1);
            } else {
                jsonObject.putOpt(Settings.KEY_PARAM_IMAGE1, pictureInfos[0].file.getAbsolutePath());
            }
            if (pictureInfos[1].file == null) {
                jsonObject.remove(Settings.KEY_PARAM_IMAGE2);
            } else {
                jsonObject.putOpt(Settings.KEY_PARAM_IMAGE2, pictureInfos[1].file.getAbsolutePath());
            }
            if (pictureInfos[2].file == null) {
                jsonObject.remove(Settings.KEY_PARAM_IMAGE3);
            } else {
                jsonObject.putOpt(Settings.KEY_PARAM_IMAGE3, pictureInfos[2].file.getAbsolutePath());
            }

            String key = getParametersSettingKey();
            Settings.getInstance().edit().putString(key, jsonObject.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "saveValues. Failed to execute putOpt.", e);
        }
    }

    private boolean isModelAllowedEmpty() {
        String selectedWorkflow = Settings.getInstance().getWorkflow("");
        WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
        if (workflow == null) {
            return true;
        }
        List<String> modelTypes = workflow.getModelTypes();
        return modelTypes == null || modelTypes.isEmpty();
    }

    public boolean isVideoWorkflow() {
        String selectedWorkflow = Settings.getInstance().getWorkflow("");
        WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
        if (workflow == null) {
            return false;
        }
        return WorkflowsResponse.Workflow.OUTPUT_VIDEO.equals(workflow.getOutputType());
    }

    public boolean isWorkflowInputImage() {
        String selectedWorkflow = Settings.getInstance().getWorkflow("");
        WorkflowsResponse.Workflow workflow = workflows.get(selectedWorkflow);
        if (workflow == null) {
            return false;
        }
        return WorkflowsResponse.Workflow.INPUT_IMAGE.equals(workflow.getInputType());
    }

    @NonNull
    private String getParameter(@Settings.ParamKeys String key) {
        if (Settings.KEY_PARAM_WORKFLOW.equals(key)) {
            return Settings.getInstance().getWorkflow("").trim();
        }
        if (Settings.KEY_PARAM_MODEL.equals(key)) {
            if (isModelAllowedEmpty()) {
                return "";
            }
            return Settings.getInstance().getModelName("").trim();
        }

        JSONObject jsonObject = getParametersJSON();
        try {
            return jsonObject.getString(key).trim();
        } catch (JSONException e) {
            Log.e(TAG, "getParameter. Failed to execute getString.", e);
        }
        return Objects.requireNonNullElse(Settings.DefaultParamMap.get(key), "");
    }

    private void showToast(String message) {
        Toast.makeText(getContentView().getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public Parameters getParameters() {
        String workflow = getParameter(Settings.KEY_PARAM_WORKFLOW);
        String modelName = getParameter(Settings.KEY_PARAM_MODEL);
        String prompt = binding.promptEditText.getText() == null ? "" : binding.promptEditText.getText().toString();
        String widthStr = getParameter(Settings.KEY_PARAM_WIDTH);
        String heightStr = getParameter(Settings.KEY_PARAM_HEIGHT);
        String seedStr = getParameter(Settings.KEY_PARAM_SEED);
        String upscaleFactorStr = getParameter(Settings.KEY_PARAM_UPSCALE_FACTOR);
        String stepStr = getParameter(Settings.KEY_PARAM_STEP);
        String cfgStr = getParameter(Settings.KEY_PARAM_CFG);

        // 验证输入
        if (workflow.isEmpty()) {
            showToast("请选择工作流");
            return null;
        }

        // 验证输入
        if (TextUtils.isEmpty(modelName) && !isModelAllowedEmpty()) {
            showToast("请选择模型");
            return null;
        }

        // 验证输入
        if (prompt.isEmpty()) {
            showToast("请输入图像描述");
            return null;
        }

        int width, height;
        try {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);

            if (width < 64 || width > 4096 || height < 64 || height > 4096) {
                showToast("图像尺寸应在 64 ~ 4096 之间");
                return null;
            }
        } catch (NumberFormatException e) {
            showToast("请输入有效的尺寸");
            return null;
        }

//        long seed = 0;
//        if (!seedStr.isEmpty()) {
//            try {
//                seed = Long.parseLong(seedStr);
//            } catch (NumberFormatException e) {
//                showToast("种子必须是数字");
//                return null;
//            }
//        }
        if (TextUtils.isEmpty(seedStr)) {
            seedStr = "0";
        }

        double upscaleFactor = 1.0;
        if (!upscaleFactorStr.isEmpty()) {
            try {
                upscaleFactor = Double.parseDouble(upscaleFactorStr);
                if (upscaleFactor < 1.0 || upscaleFactor > 4.0) {
                    showToast("放大系数应在 1.0 ~ 4.0 之间");
                    return null;
                }
            } catch (NumberFormatException e) {
                showToast("放大系数必须是数字");
                return null;
            }
        }

        int step = 20;
        if (!stepStr.isEmpty()) {
            try {
                step = Integer.parseInt(stepStr);
            } catch (NumberFormatException e) {
                showToast("种子必须是数字");
                return null;
            }
        }

        double cfg = 8.0;
        if (!cfgStr.isEmpty()) {
            try {
                cfg = Double.parseDouble(cfgStr);
                if (cfg < 0.1 || cfg > 100.0) {
                    showToast("CFG应在 0.1 ~ 100.0 之间");
                    return null;
                }
            } catch (NumberFormatException e) {
                showToast("CFG必须是数字");
                return null;
            }
        }

        // 创建参数对象
        Parameters parameters = new Parameters(workflow, modelName, prompt, seedStr, width, height, step, cfg, upscaleFactor);

        int seconds = 0;
        if (isVideoWorkflow()) {
            String secondsStr = getParameter(Settings.KEY_PARAM_SECONDS);
            if (!secondsStr.isEmpty()) {
                try {
                    seconds = Integer.parseInt(secondsStr);
                    if (seconds < 1 || seconds > 8) {
                        showToast("时长应在 1 ~ 8 之间");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showToast("时长必须是数字");
                    return null;
                }
            }
            parameters.setSeconds(seconds);
        }

        if (isWorkflowInputImage()) {
            String megapixelsStr = getParameter(Settings.KEY_PARAM_MEGAPIXELS);
            try {
                double megapixels = Double.parseDouble(megapixelsStr);
                if (megapixels < 0.1 || megapixels > 5.0) {
                    showToast("像素值应在 0.1 ~ 5.0 之间");
                    return null;
                }
                parameters.setMegapixels(megapixels);
            } catch (NumberFormatException e) {
                showToast("像素值必须是数字");
                return null;
            }

            if (pictureInfos[0].file == null || !pictureInfos[0].file.exists()) {
                showToast("必须指定图1");
                return null;
            }

            File[] imageFiles = new File[pictureInfos.length];
            for (int i = 0; i < pictureInfos.length; i++) {
                imageFiles[i] = pictureInfos[i].file;
            }
            parameters.setImageFiles(imageFiles);
        }

        return parameters;
    }

    public interface OnSubmitCallback extends Callbacks.Callback {
        @Override
        void apply();
    }

    private class WorkflowAdapter extends BaseAdapter {

        private final List<String> items;
        private final Context context;

        public WorkflowAdapter(@NonNull Context context) {
            this.context = context.getApplicationContext();
            items = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutWorkflowItemBinding binding;
            if (convertView == null) {
                binding = LayoutWorkflowItemBinding.inflate(LayoutInflater.from(context), parent, false);
                convertView = binding.getRoot();
                convertView.setTag(binding);
            } else {
                binding = (LayoutWorkflowItemBinding) convertView.getTag();
            }
            String workflowKey = getItem(position);
            if (workflowKey != null) {
                WorkflowsResponse.Workflow workflow = workflows.get(workflowKey);
                if (workflow != null) {
                    binding.text1.setText(workflow.getDisplayName());
                }
            }
            return convertView;
        }

        public void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        public void addAll(List<String> workflowNames) {
            items.addAll(workflowNames);
            notifyDataSetChanged();
        }
    }

    public interface DataRequirer {
        void loadWorkflows();
        void loadModels(@NonNull List<String> modelTypes);
        void requirePicture(int which, boolean useCamera);
    }

    private class PictureInfo {
        private Bitmap bmp;
        private File file;

        private void setFile(File file) {
            this.file = file;
            File thumbnail = null;
            if (file != null) {
                thumbnail = ImageUtils.getThumbnailFileInCacheDir(getContentView().getContext(), file);
                if (!thumbnail.exists()) {
                    float width = getContentView().getContext().getResources().getDimension(R.dimen.thumbnail_width);
                    float height = getContentView().getContext().getResources().getDimension(R.dimen.thumbnail_height);
                    thumbnail = ImageUtils.createAndSaveThumbnail(file, thumbnail, width, height);
                }
            }
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
            if (thumbnail != null && thumbnail.exists()) {
                bmp = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
            } else {
                bmp = null;
            }
        }
    }
}
