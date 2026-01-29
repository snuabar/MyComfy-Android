package com.snuabar.mycomfy.main.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.snuabar.mycomfy.utils.ImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DataIO {

    private static final String TAG = DataIO.class.getName();

    public static final String PREFIX = "MyComfy_";
    public static final String MSG_EXT = ".msg";
    public static final String IMG_EXT = ".png";

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

    @NonNull
    public static List<AbstractMessageModel> copyMessageModels(Context context) {
        File msgDir = getMsgDir(context);

        ArrayList<AbstractMessageModel> models = new ArrayList<>();
        File[] files = msgDir.listFiles((dir, name) -> name.startsWith(PREFIX) && name.endsWith(MSG_EXT));
        if (files == null) {
            return models;
        }

        for (File file : files) {
            AbstractMessageModel model = readModel(file);
            if (model != null) {
                models.add(model);
            }
        }

        return models;
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

    //region 模型读写

    public static void writeModelFile(Context context, AbstractMessageModel model) {
        if (TextUtils.isEmpty(model.id)) {
            model.id = UUID.randomUUID().toString().replaceAll("-", "");
        }

        File msgDir = getMsgDir(context);
        String fileName = PREFIX + model.id + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            oos.writeObject(model);
            oos.flush();
        } catch (IOException e) {
            modelFile.delete();
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteModelFile(Context context, AbstractMessageModel model) {
        if (TextUtils.isEmpty(model.id)) {
            model.id = UUID.randomUUID().toString().replaceAll("-", "");
        }

        File msgDir = getMsgDir(context);
        String fileName = PREFIX + model.id + MSG_EXT;
        File modelFile = new File(msgDir, fileName);

        if (modelFile.delete()) {
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

    public static AbstractMessageModel readModel(File modelFile) {
        if (modelFile.exists() && modelFile.isFile()) {
            AbstractMessageModel model;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile))) {
                model = (AbstractMessageModel) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return model;
        }
        return null;
    }

    //endregion

}
