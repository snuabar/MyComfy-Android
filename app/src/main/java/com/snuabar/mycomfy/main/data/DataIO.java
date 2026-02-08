package com.snuabar.mycomfy.main.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snuabar.mycomfy.main.model.I2IReceivedMessageModel;
import com.snuabar.mycomfy.main.model.I2ISentMessageModel;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.ReceivedVideoMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.main.model.SentVideoMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleReceivedMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleSentMessageModel;
import com.snuabar.mycomfy.utils.FileOperator;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.TextCompressor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataIO {

    private static final String TAG = DataIO.class.getName();
    private final static String CLASS_IDENTITY_KEY = "class.identify.key.to.create.class.from.json";

    private static final String PREFIX = "MyComfy_";
    private static final String MSG_EXT = ".msg";
    private static final String IMG_EXT = ".png";
    private static final String VIDEO_EXT = ".mp4";
    private List<AbstractMessageModel> sharedMessageModels;
    private final Context context;

    private static DataIO Instance = null;

    public static DataIO init(Context context) {
        if (Instance == null) {
            Instance = new DataIO(context);
        }
        return Instance;
    }

    public static DataIO getInstance() {
        if (Instance == null) {
            throw new NullPointerException("Has not initialized. Initialize DataIO by calling DataIO.init(<context>)");
        }
        return Instance;
    }

    private DataIO(Context context) {
        this.context = context.getApplicationContext();
    }

    void setSharedMessageModels(List<AbstractMessageModel> sharedMessageModels) {
        this.sharedMessageModels = sharedMessageModels;
    }

    private File getOutputDir() {
        File file = context.getExternalFilesDir(null);
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getOutputDir: failed to execute mkdirs()");
        }
        file = context.getExternalFilesDir("output");
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getOutputDir: failed to execute mkdirs()");
        }
        return file;
    }

    private File getMsgDir() {
        File file = context.getExternalFilesDir(null);
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getMessageDir: failed to execute mkdirs()");
        }
        file = context.getExternalFilesDir("msg");
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getMessageDir: failed to execute mkdirs()");
        }
        return file;
    }

    @NonNull
    List<AbstractMessageModel> loadMessageModels() {
        File msgDir = getMsgDir();

        ArrayList<AbstractMessageModel> models = new ArrayList<>();
        File[] files = msgDir.listFiles((dir, name) -> name.startsWith(PREFIX) && name.endsWith(MSG_EXT));
        if (files != null) {
            for (File file : files) {
                AbstractMessageModel model = readModelFile(file);
                if (model != null) {
                    models.add(model);
                }
            }
        }
        models.sort(Comparator.comparingLong(AbstractMessageModel::getUTCTimestamp));
        return models;
    }

    @NonNull
    public List<AbstractMessageModel> copyMessageModels() {
        return new ArrayList<>(sharedMessageModels);
    }

    public File newImageFile() {
        File outputDir = getOutputDir();
        assert outputDir != null;
        // 创建保存目录
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "newImageFile: failed to execute mkdirs()");
        }
        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = PREFIX + timeStamp + IMG_EXT;
        return new File(outputDir, fileName);
    }

    public File newVideoFile() {
        File outputDir = getOutputDir();
        assert outputDir != null;
        // 创建保存目录
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "newVideoFile: failed to execute mkdirs()");
        }
        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = PREFIX + timeStamp + VIDEO_EXT;
        return new File(outputDir, fileName);
    }

    public File copyImageFile(File imageFile) {
        File newImageFile = newImageFile();
        if (FileOperator.copyFile(imageFile, newImageFile)) {
            return newImageFile;
        }
        if (FileOperator.copyFileTraditional(imageFile, newImageFile)) {
            return newImageFile;
        }
        return null;
    }

    //region 模型读写

    AbstractMessageModel writeModelFile(AbstractMessageModel model) {
        File msgDir = getMsgDir();
        String fileName = PREFIX + model.getId() + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        JSONObject jsonObject = model.toJson();
        if (jsonObject != null) {
            try {
                jsonObject.putOpt(CLASS_IDENTITY_KEY, model.getClass().getName());
            } catch (JSONException e) {
                Log.e(TAG, "writeModelFile: failed to execute putOpt.");
            }
            String jsonStr = jsonObject.toString();
            byte[] bytes = TextCompressor.INSTANCE.compress(jsonStr);
            try (FileOutputStream fos = new FileOutputStream(modelFile)) {
                fos.write(bytes);
                fos.flush();
            } catch (IOException e) {
                Log.e(TAG, "writeModelFile: failed to output model.");
                return model;
            }
        }

        return readModelFile(modelFile);
    }

    private AbstractMessageModel readModelFile(File modelFile) {
        if (modelFile.exists() && modelFile.isFile()) {
            try {
                byte[] bytes = Files.readAllBytes(modelFile.toPath());
                String jsonStr = TextCompressor.INSTANCE.decompress(bytes);
                JSONObject jsonObject = new JSONObject(jsonStr);
                return createModel(jsonObject);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "readModelFile: failed to read model.");
            }
        }
        return null;
    }

    @Nullable
    private AbstractMessageModel createModel(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.has(CLASS_IDENTITY_KEY)) {
            String className = jsonObject.optString(CLASS_IDENTITY_KEY, null);
            // Received
            if (ReceivedMessageModel.class.getName().equals(className)) {
                return new ReceivedMessageModel(jsonObject);
            }
            if (UpscaleReceivedMessageModel.class.getName().equals(className)) {
                return new UpscaleReceivedMessageModel(jsonObject);
            }
            if (ReceivedVideoMessageModel.class.getName().equals(className)) {
                return new ReceivedVideoMessageModel(jsonObject);
            }
            if (I2IReceivedMessageModel.class.getName().equals(className)) {
                return new I2IReceivedMessageModel(jsonObject);
            }
            // Sent
            if (SentMessageModel.class.getName().equals(className)) {
                return new SentMessageModel(jsonObject);
            }
            if (UpscaleSentMessageModel.class.getName().equals(className)) {
                return new UpscaleSentMessageModel(jsonObject);
            }
            if (SentVideoMessageModel.class.getName().equals(className)) {
                return new SentVideoMessageModel(jsonObject);
            }
            if (I2ISentMessageModel.class.getName().equals(className)) {
                return new I2ISentMessageModel(jsonObject);
            }
        }
        return null;
    }

    boolean deleteModelFile(AbstractMessageModel model) {

        File msgDir = getMsgDir();
        String fileName = PREFIX + model.getId() + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        if (modelFile.delete()) {
            try {
                // 删除图像和缩略图
                if (model.getImageFile() != null && model.getImageFile().exists()) {
                    File thumbFile = ImageUtils.getThumbnailFile(model.getImageFile());
                    model.getImageFile().delete();
                    if (thumbFile.exists()) {
                        thumbFile.delete();
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "deleteModelFile: failed to read model.");
            }
            return true;
        }
        return false;
    }

    //endregion

}
