package com.snuabar.mycomfy.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.utils.TextCompressor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Settings {
    public static final String TAG = Settings.class.getName();

    public static final String KEY_PARAM_WORKFLOW = "workflow";
    public static final String KEY_PARAM_MODEL = "model";
    public static final String KEY_PARAM_WIDTH = "width";
    public static final String KEY_PARAM_HEIGHT = "height";
    public static final String KEY_PARAM_SEED = "seed";
    public static final String KEY_PARAM_SEED_CTL = "seed_ctl";
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
            KEY_PARAM_SEED_CTL,
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
            KEY_PARAM_SECONDS, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getSeconds()),
            KEY_PARAM_MEGAPIXELS, String.valueOf(WorkflowsResponse.DefaultParameters.Default.getMegapixels())
    );

    public static final String KEY_DATA_IMPORTED = "data_imported";
    public static final String KEY_PROMPT = "prompt";
    public static final String KEY_WORKFLOW_DISPLAY_NAMES = "workflow_display_names";

    private final SharedPreferences preferences;

    private static Settings Instance;

    public static Settings init(Context context) {
        Instance = new Settings(context);
        return Instance;
    }

    public static Settings getInstance() {
        return Instance;
    }

    private final Context context;
    private final String prefsName;
    private SharedPreferences.Editor editor = null;

    private Settings(Context context) {
        this.context = context;
        this.prefsName = context.getPackageName() + ".main.settings";
        preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    private File getExternalSharedPrefsDir() {
        File file = context.getExternalFilesDir(null);
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getExternalSharedPrefsDir: failed to execute mkdirs()");
        }
        file = context.getExternalFilesDir("shared_prefs");
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getExternalSharedPrefsDir: failed to execute mkdirs()");
        }
        return file;
    }

    private File getExternalSharedPrefsFile() {
        File externalSharedPrefsDir = getExternalSharedPrefsDir();
        return new File(externalSharedPrefsDir, prefsName + ".json");
    }

    public void backup() throws JSONException, IOException {
        File file = getExternalSharedPrefsFile();
        JSONObject jsonObject = new JSONObject();
        Map<String, ?> all = preferences.getAll();
        for (String key : all.keySet()) {
            Object object = all.get(key);
            if (object instanceof Set<?>) {
                JSONArray jsonArray = new JSONArray();
                for (Object str : (Set<?>)object) {
                    jsonArray.put(str);
                }
                jsonObject.putOpt(key, jsonArray);
            } else {
                jsonObject.putOpt(key, object);
            }
        }
        String jsonStr = jsonObject.toString();
        byte[] bytes = TextCompressor.INSTANCE.compress(jsonStr);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        }
    }

    public void restoreBackup() throws IOException, JSONException {
        File file = getExternalSharedPrefsFile();
        if (file.exists() && file.isFile()) {
            SharedPreferences.Editor editor = preferences.edit();
            byte[] bytes = Files.readAllBytes(file.toPath());
            String jsonStr = TextCompressor.INSTANCE.decompress(bytes);
            JSONObject jsonObject = new JSONObject(jsonStr);
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                Object object = jsonObject.opt(key);
                if (object instanceof Integer) {
                    editor.putInt(key, (int) object);
                } else if (object instanceof Boolean) {
                    editor.putBoolean(key, (boolean) object);
                } else if (object instanceof Float) {
                    editor.putFloat(key, (float) object);
                } else if (object instanceof Long) {
                    editor.putLong(key, (long) object);
                } else if (object instanceof String) {
                    editor.putString(key, Common.correctPackageLikeStringsForDebug((String) object));
                } else if (object instanceof JSONArray){
                    Set<String> stringSet = new HashSet<>();
                    JSONArray jsonArray = (JSONArray) object;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        stringSet.add(Common.correctPackageLikeStringsForDebug(jsonArray.getString(i)));
                    }
                    editor.putStringSet(key, stringSet);
                }
            }
            editor.apply();
        }
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

    public Settings setWorkflowDisplayNames(Map<String, String> displayNames) {
        JSONObject jsonObject = new JSONObject(displayNames);
        String jsonString = jsonObject.toString();
        editor.putString(KEY_WORKFLOW_DISPLAY_NAMES, jsonString);
        return this;
    }

    @NonNull
    public String getWorkflowDisplayName(String workflow) {
        String jsonString = getString(KEY_WORKFLOW_DISPLAY_NAMES, workflow);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonObject.optString(workflow, workflow);
            } catch (JSONException e) {
                Log.e(TAG, "getWorkflowDisplayName: failed to execute optString()");
            }
        }
        return workflow;
    }
}
