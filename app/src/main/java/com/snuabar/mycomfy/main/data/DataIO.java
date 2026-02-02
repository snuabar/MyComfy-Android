package com.snuabar.mycomfy.main.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataIO {

    private static final String TAG = DataIO.class.getName();

    public static final String PREFIX = "MyComfy_";
    public static final String MSG_EXT = ".msg";
    public static final String IMG_EXT = ".png";
    private static final List<AbstractMessageModel> MessageModels = new ArrayList<>();
    private static final Map<String, Integer> IdToIndexMap = new HashMap<>();

    public static File getOutputDir(Context context) {
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

    public static File getMsgDir(Context context) {
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

    private static void refreshIdToIndexMap() {
        IdToIndexMap.clear();
        for (int i = 0; i < MessageModels.size(); i++) {
            IdToIndexMap.put(MessageModels.get(i).getId(), i);
        }
    }

    public static int getIndexWithId(String messageModelId) {
        Integer index = IdToIndexMap.get(messageModelId);
        return index == null ? -1 : index;
    }

    @NonNull
    public static List<AbstractMessageModel> copyMessageModels(Context context) {
        if (MessageModels.isEmpty()) {
            File msgDir = getMsgDir(context);

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
            MessageModels.addAll(models);
            MessageModels.sort(Comparator.comparingLong(AbstractMessageModel::getUTCTimestamp));
            refreshIdToIndexMap();
        }
        return new ArrayList<>(MessageModels);
    }

    public static File newImageFile(Context context) {
        File outputDir = getOutputDir(context);
        assert outputDir != null;
        // 创建保存目录
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "saveImageToFile: failed to execute mkdirs()");
        }
        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = PREFIX + timeStamp + IMG_EXT;
        return new File(outputDir, fileName);
    }

    public static File copyImageFile(Context context, File imageFile) {
        File newImageFile = newImageFile(context);
        if (FileOperator.copyFile(imageFile, newImageFile)) {
            return newImageFile;
        }
        if (FileOperator.copyFileTraditional(imageFile, newImageFile)) {
            return newImageFile;
        }
        return null;
    }

    //region 模型读写

    public static void writeModelFile(Context context, AbstractMessageModel model) {
        File msgDir = getMsgDir(context);
        String fileName = PREFIX + model.getId() + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        JSONObject jsonObject = model.toJson();
        if (jsonObject != null) {
            String jsonStr = jsonObject.toString();
            byte[] bytes = TextCompressor.INSTANCE.compress(jsonStr);
            try (FileOutputStream fos = new FileOutputStream(modelFile)) {
                fos.write(bytes);
                fos.flush();
            } catch (IOException e) {
                Log.e(TAG, "writeModelFile: failed to output model.");
                return;
            }

            // 更新缓存列表
            int index = getIndexWithId(model.getId());
            if (index == -1) {
                MessageModels.add(readModelFile(modelFile));
                refreshIdToIndexMap();
            } else {
                MessageModels.set(index, readModelFile(modelFile));
            }
        }
    }

    private static AbstractMessageModel readModelFile(File modelFile) {
        if (modelFile.exists() && modelFile.isFile()) {
            try {
                byte[] bytes = Files.readAllBytes(modelFile.toPath());
                String jsonStr = TextCompressor.INSTANCE.decompress(bytes);
                JSONObject jsonObject = new JSONObject(jsonStr);
                return AbstractMessageModel.Create(jsonObject);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "readModelFile: failed to read model.");
            }
        }
        return null;
    }

    public static boolean deleteModelFile(Context context, AbstractMessageModel model) {

        File msgDir = getMsgDir(context);
        String fileName = PREFIX + model.getId() + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        if (modelFile.delete()) {
            // 更新缓存列表
            int index = getIndexWithId(model.getId());
            if (index >= 0) {
                MessageModels.remove(index);
                refreshIdToIndexMap();
            }
            // 删除图像和缩略图
            if (model.getImageFile() != null && model.getImageFile().exists()) {
                File thumbFile = ImageUtils.getThumbnailFile(model.getImageFile());
                model.getImageFile().delete();
                if (thumbFile.exists()) {
                    thumbFile.delete();
                }
            }
            return true;
        }
        return false;
    }

    //endregion

}
