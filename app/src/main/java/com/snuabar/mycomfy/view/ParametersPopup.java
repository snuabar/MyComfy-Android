package com.snuabar.mycomfy.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.appcompat.content.res.AppCompatResources;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.ModelResponse;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.databinding.LayoutParametersPopupWindowBinding;
import com.snuabar.mycomfy.setting.Settings;
import com.snuabar.mycomfy.utils.ViewUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParametersPopup extends PopupWindow {

    private final static String TAG = ParametersPopup.class.getName();

    private final LayoutParametersPopupWindowBinding binding;
    private final RetrofitClient retrofitClient;
    private final Map<String, List<String>> workflows = new HashMap<>();
    private final List<String> models = new ArrayList<>();
    private ArrayAdapter<String> workflowAdapter, modelAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private JSONObject paramJsonObject = null;
    private String paramKey = null;
    private static final Random random = new Random();

    public ParametersPopup(Context context) {
        super(context);
        binding = LayoutParametersPopupWindowBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.parameters_popup_bg));
        setElevation(8);
        retrofitClient = RetrofitClient.getInstance();
        loadValues();
        // 设置按钮点击事件
        setupClickListeners();
        // 设置工作流控件
        setupAdapters();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        // 手动测量和布局
        ViewUtils.measure(getContentView(), 300);
        super.showAsDropDown(anchor, 0, -anchor.getHeight() - getContentView().getMeasuredHeight(), Gravity.TOP | Gravity.START);
        binding.btnSwitchWH.setEnabled(false);
        handler.post(this::loadWorkflows);
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
    }

    public void randomSeed(boolean save) {
        random.setSeed(Clock.systemUTC().millis());
        int seed = Math.abs(random.nextInt());
        binding.etSeed.setText(String.valueOf(seed));
        if (save) {
            saveValues();
        }
    }

    private void switchWidthAndHeight() {
        String widthStr = binding.etWidth.getText().toString();
        String heightStr = binding.etHeight.getText().toString();
        binding.etWidth.setText(heightStr);
        binding.etHeight.setText(widthStr);
    }

    private void setupAdapters() {
        workflowAdapter = new ArrayAdapter<>(getContentView().getContext(), R.layout.layout_workflow_item, R.id.text1);
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

    private void loadWorkflows() {
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
        List<String> modelTypes = workflows.get(selectedWorkflow);
        if (modelTypes == null) {
            loadValues();
            return;
        }

        binding.tableRowModels.setAlpha(modelTypes.isEmpty() ? 0.3f : 1.0f);
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
                paramJsonObject = new JSONObject(Settings.DefaultParamMap);
            }
        }
        return paramJsonObject;
    }

    private void loadValues() {
        JSONObject jsonObject = getParametersJSON();
        try {
            binding.etWidth.setText(jsonObject.getString(Settings.KEY_PARAM_WIDTH));
            binding.etHeight.setText(jsonObject.getString(Settings.KEY_PARAM_HEIGHT));
            binding.etSeed.setText(jsonObject.getString(Settings.KEY_PARAM_SEED));
            binding.etStep.setText(jsonObject.getString(Settings.KEY_PARAM_STEP));
            binding.etCFG.setText(jsonObject.getString(Settings.KEY_PARAM_CFG));
            binding.etUpscaleFactor.setText(jsonObject.getString(Settings.KEY_PARAM_UPSCALE_FACTOR));
        } catch (JSONException e) {
            Log.e(TAG, "loadValues. Failed to execute getXXX.", e);
        }
        binding.btnSwitchWH.setEnabled(true);
    }

    private void saveValues() {
        JSONObject jsonObject = getParametersJSON();
        try {
            jsonObject.putOpt(Settings.KEY_PARAM_WIDTH, binding.etWidth.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_HEIGHT, binding.etHeight.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_SEED, binding.etSeed.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_STEP, binding.etStep.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_CFG, binding.etCFG.getText().toString());
            jsonObject.putOpt(Settings.KEY_PARAM_UPSCALE_FACTOR, binding.etUpscaleFactor.getText().toString());

            String key = getParametersSettingKey();
            Settings.getInstance().edit().putString(key, jsonObject.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "saveValues. Failed to execute putOpt.", e);
        }
    }

    public boolean isModelAllowedEmpty() {
        String selectedWorkflow = Settings.getInstance().getWorkflow("");
        List<String> modelTypes = workflows.get(selectedWorkflow);
        return modelTypes == null || modelTypes.isEmpty();
    }

    public String getParameter(@Settings.ParamKeys String key) {
        if (Settings.KEY_PARAM_WORKFLOW.equals(key)) {
            return Settings.getInstance().getWorkflow("").trim();
        }
        if (Settings.KEY_PARAM_MODEL.equals(key)) {
            String selectedWorkflow = Settings.getInstance().getWorkflow("");
            List<String> modelTypes = workflows.get(selectedWorkflow);
            if (modelTypes == null || modelTypes.isEmpty()) {
                return null;
            }
            return Settings.getInstance().getModelName("").trim();
        }
        JSONObject jsonObject = getParametersJSON();
        try {
            return jsonObject.getString(key).trim();
        } catch (JSONException e) {
            Log.e(TAG, "getParameter. Failed to execute getString.", e);
        }
        return Settings.DefaultParamMap.get(key);
    }
}
