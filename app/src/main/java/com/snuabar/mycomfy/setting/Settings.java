package com.snuabar.mycomfy.setting;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringDef;

import com.snuabar.mycomfy.client.WorkflowsResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public class Settings {
    public static final String KEY_PARAM_WORKFLOW = "workflow";
    public static final String KEY_PARAM_MODEL = "model";
    public static final String KEY_PARAM_WIDTH = "width";
    public static final String KEY_PARAM_HEIGHT = "height";
    public static final String KEY_PARAM_SEED = "seed";
    public static final String KEY_PARAM_STEP = "step";
    public static final String KEY_PARAM_CFG = "cfg";
    public static final String KEY_PARAM_UPSCALE_FACTOR = "upscale_factor";
    public static final String KEY_PARAM_SECONDS = "seconds";
    public static final String KEY_PARAM_MEGAPIXELS = "megapixels";
    public static final String KEY_PARAM_IMAGE1 = "image1";
    public static final String KEY_PARAM_IMAGE2 = "image2";
    public static final String KEY_PARAM_IMAGE3 = "image3";

    @StringDef({
            KEY_PARAM_WORKFLOW,
            KEY_PARAM_MODEL,
            KEY_PARAM_WIDTH,
            KEY_PARAM_HEIGHT,
            KEY_PARAM_SEED,
            KEY_PARAM_STEP,
            KEY_PARAM_CFG,
            KEY_PARAM_UPSCALE_FACTOR,
            KEY_PARAM_SECONDS,
            KEY_PARAM_MEGAPIXELS,
            KEY_PARAM_IMAGE1,
            KEY_PARAM_IMAGE2,
            KEY_PARAM_IMAGE3
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ParamKeys {

    }

    // 1.0 means no upscale.
    public static final Map<String, String> DefaultParamMap = Map.of(
            KEY_PARAM_WIDTH, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getWidth()),
            KEY_PARAM_HEIGHT, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getHeight()),
            KEY_PARAM_SEED, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getSeed()),
            KEY_PARAM_STEP, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getStep()),
            KEY_PARAM_CFG, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getCfg()),
            KEY_PARAM_UPSCALE_FACTOR, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getUpscale_factor()),
            KEY_PARAM_SECONDS, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getSeconds())
    );

    public static final String KEY_DATA_IMPORTED = "data_imported";
    public static final String KEY_PROMPT = "prompt";

    private final SharedPreferences preferences;

    private static Settings Instance;

    public static Settings init(Context context) {
        Instance = new Settings(context);
        return Instance;
    }

    public static Settings getInstance() {
        return Instance;
    }

    private SharedPreferences.Editor editor = null;

    private Settings(Context context) {
        preferences = context.getSharedPreferences(context.getPackageName() + ".main.settings", Context.MODE_PRIVATE);
    }

    public Settings edit() {
        editor = preferences.edit();
        return this;
    }

    public void apply() {
        editor.apply();
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public Settings putString(String key, String defValue) {
        editor.putString(key, defValue);
        return this;
    }

    public Settings putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        return this;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public String getPrompt(String defValue) {
        return preferences.getString(KEY_PROMPT, defValue);
    }

    public void setPrompt(String prompt) {
        edit().putString(KEY_PROMPT, prompt).apply();
    }

    public void setDataImportedState() {
        edit().putBoolean(KEY_DATA_IMPORTED, true).apply();
    }

    public void clearDataImportedState() {
        edit().putBoolean(KEY_DATA_IMPORTED, false).apply();
    }

    public boolean hasDataImportedState() {
        return getBoolean(KEY_DATA_IMPORTED, false);
    }

    public Settings setWorkflow(String workflow) {
        editor.putString(KEY_PARAM_WORKFLOW, workflow);
        return this;
    }

    public String getWorkflow(String defValue) {
        return getString(KEY_PARAM_WORKFLOW, defValue);
    }

    public Settings setModelName(String modelName) {
        editor.putString(KEY_PARAM_MODEL, modelName);
        return this;
    }

    public String getModelName(String defValue) {
        return getString(KEY_PARAM_MODEL, defValue);
    }
}
