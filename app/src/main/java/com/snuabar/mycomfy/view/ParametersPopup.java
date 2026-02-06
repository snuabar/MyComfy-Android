package com.snuabar.mycomfy.view;

import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.ModelResponse;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.databinding.LayoutParametersPopupWindowBinding;
import com.snuabar.mycomfy.databinding.LayoutWorkflowItemBinding;
import com.snuabar.mycomfy.main.data.prompt.PromptManager;
import com.snuabar.mycomfy.setting.Settings;
import com.snuabar.mycomfy.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParametersPopup extends GeneralPopup {

    private final static String TAG = ParametersPopup.class.getName();

    private WeakReference<View> mAnchor;
    private final LayoutParametersPopupWindowBinding binding;
    private final RetrofitClient retrofitClient;
    private final Map<String, WorkflowsResponse.Workflow> workflows = new HashMap<>();
    private final List<String> models = new ArrayList<>();
    private WorkflowAdapter workflowAdapter;
    private ArrayAdapter<String> modelAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private JSONObject paramJsonObject = null;
    private String paramKey = null;
    private static final Random random = new Random();
    private final Stack<String> undoList;
    private OnSubmitCallback onSubmitCallback;

    public ParametersPopup(Context context) {
        super(context);
        undoList = new Stack<>();
        binding = LayoutParametersPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        retrofitClient = RetrofitClient.getInstance();
        // 设置按钮点击事件
        setupClickListeners();
        // 设置工作流控件
        setupAdapters();
        handler.post(this::loadWorkflows);

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
        PromptManager.Companion.getInstance().getAllCategories().forEach(c -> addChip(c.getName(), c.getDisplayName()));
        loadPrompt();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        // 手动测量和布局
        ViewUtils.measure(getContentView(), Integer.MAX_VALUE);
        mAnchor = new WeakReference<>(anchor);
        super.showAsDropDown(anchor, 0, -anchor.getHeight() - getContentView().getMeasuredHeight(), Gravity.TOP | Gravity.START);
        binding.btnSwitchWH.setEnabled(false);
        handler.post(this::loadWorkflows);
    }

    @Override
    public void update() {
        if (mAnchor != null && mAnchor.get() != null) {
            // 手动测量和布局
            ViewUtils.measure(getContentView(), 0);
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

    private void setupClickListeners() {
        // 更新工作流按扭
        binding.btnLoadWorkflows.setOnClickListener(v -> loadWorkflows());
        // 更新工作流按扭
        binding.btnLoadModels.setOnClickListener(v -> loadModels());
        binding.btnSwitchWH.setOnClickListener(v -> switchWidthAndHeight());
        binding.btnRandom.setOnClickListener(v -> {
            random.setSeed(Clock.systemUTC().millis());
            int seed = Math.abs(random.nextInt());
            binding.etSeed.setText(String.valueOf(seed));
        });
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnSubmit.setOnClickListener(v -> {
            dismiss();
            if (onSubmitCallback != null) {
                onSubmitCallback.apply();
            }
        });
    }

    public void setOnSubmitCallback(OnSubmitCallback callback) {
        this.onSubmitCallback = callback;
    }

    public void randomSeed(boolean save) {
        random.setSeed(Clock.systemUTC().millis());
        int seed = Math.abs(random.nextInt());
        binding.etSeed.setText(String.valueOf(seed));
        if (save) {
            saveValues();
        }
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
                Settings.getInstance().edit().setModelName(selectedModel).apply();
                loadValues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void loadWorkflows() {
        // 发送请求
        retrofitClient.getApiService().loadWorkflow().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WorkflowsResponse> call, @NonNull Response<WorkflowsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WorkflowsResponse workflowsResponse = response.body();
                    workflows.clear();
                    workflows.putAll(workflowsResponse.getWorkflows());
                    handler.post(() -> {
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
                    });
                } else {
                    Log.e(TAG, "请求失败，状态码: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WorkflowsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "请求失败。" + t.getMessage(), t);
            }
        });
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

        Log.i(TAG, "发送请求: 加载模型列表。");
        for (String modelType : modelTypes) {
            // 发送请求
            retrofitClient.getApiService().loadModels(modelType).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse> call, @NonNull Response<ModelResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ModelResponse modelResponse = response.body();
                        models.clear();
                        models.addAll(modelResponse.getModels());
                        models.removeIf(m -> workflow.getExcludeModels().stream().anyMatch(m::contains));
                        handler.post(() -> {
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
                        });
                    } else {
                        Log.e(TAG, "请求失败，状态码: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "请求失败: " + t.getMessage());
                }
            });
        }
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

        int visibility = binding.rowSeconds.getVisibility();
        binding.rowSeconds.setVisibility(isVideoWorkflow() ? View.VISIBLE : View.GONE);
        if (binding.rowSeconds.getVisibility() != visibility) {
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
            jsonObject.putOpt(Settings.KEY_PARAM_STEP, binding.etStep.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_CFG, binding.etCFG.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_UPSCALE_FACTOR, binding.etUpscaleFactor.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_SECONDS, binding.etSeconds.getText().toString());

            String key = getParametersSettingKey();
            Settings.getInstance().edit().putString(key, jsonObject.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "saveValues. Failed to execute putOpt.", e);
        }
    }

    public boolean isModelAllowedEmpty() {
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
        String secondsStr = getParameter(Settings.KEY_PARAM_SECONDS);

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

        Integer seed = null;
        if (!seedStr.isEmpty()) {
            try {
                seed = Integer.parseInt(seedStr);
            } catch (NumberFormatException e) {
                showToast("种子必须是数字");
                return null;
            }
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

        int seconds = 0;
        if (isVideoWorkflow()) {
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
        }

        // 创建参数对象
        Parameters parameters = new Parameters(workflow, modelName, prompt, seed, width, height, step, cfg, upscaleFactor);
        if (seconds != 0) {
            parameters.setSeconds(seconds);
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
}
