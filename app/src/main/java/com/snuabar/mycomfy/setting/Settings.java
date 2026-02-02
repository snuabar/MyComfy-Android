package com.snuabar.mycomfy.setting;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringDef;

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

    @StringDef({KEY_PARAM_WORKFLOW, KEY_PARAM_MODEL, KEY_PARAM_WIDTH, KEY_PARAM_HEIGHT, KEY_PARAM_SEED, KEY_PARAM_STEP, KEY_PARAM_CFG, KEY_PARAM_UPSCALE_FACTOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ParamKeys {

    }

    // 1.0 means no upscale.
    public static final Map<String, String> DefaultParamMap = Map.of(
            KEY_PARAM_WIDTH, "512",
            KEY_PARAM_HEIGHT, "512",
            KEY_PARAM_SEED, "0",
            KEY_PARAM_STEP, "20",
            KEY_PARAM_CFG, "8.0",
            KEY_PARAM_UPSCALE_FACTOR, "1.0"
    );

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

    public SharedPreferences getPreferences() {
        return preferences;
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
