package com.snuabar.mycomfy.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Output {

    private static final String TAG = Output.class.getName();

    public static final String PREFIX = "MyComfy_";
    public static final String EXT = ".png";
    public static final String PROMPT_EXT = ".prompt";

    public static File getOutputDir(Context context) {
        return context.getExternalFilesDir("output");
    }

    public static ArrayList<ImageUtils.ImageContent> getOutputFiles(Context context) {
        File outputDir = getOutputDir(context);

        File[] files = outputDir.listFiles((dir, name) -> name.startsWith(PREFIX) && name.endsWith(EXT));
        if (files == null) {
            return null;
        }

        ArrayList<ImageUtils.ImageContent> finalFiles = new ArrayList<>();
        for (File imageFile : files) {
            finalFiles.add(new ImageUtils.ImageContent(imageFile, new File(imageFile.getAbsolutePath() + PROMPT_EXT)));
        }

        return finalFiles;
    }

    public static File[] newOutputFile(Context context) {
        File outputDir = getOutputDir(context);
        assert outputDir != null;
        // 创建保存目录
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "saveImageToFile: failed to execute mkdirs()");
        }
        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = PREFIX + timeStamp + EXT;
        return new File[]{new File(outputDir, fileName), new File(outputDir, fileName + PROMPT_EXT)};
    }

    public static void clean(Context context) {
        ArrayList<ImageUtils.ImageContent> imageContents = getOutputFiles(context);
        if (imageContents != null) {
            for (ImageUtils.ImageContent content : imageContents) {
                try {
                    if (!content.imageFile.delete()) {
                        Log.e(TAG, "文件无法删除: " + content.imageFile.getName());
                    }
                    if (!content.promptFile.delete()) {
                        Log.e(TAG, "文件无法删除: " + content.promptFile.getName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "某些文件无法删除: " + e.getMessage());
                }
            }
        }
    }

}
