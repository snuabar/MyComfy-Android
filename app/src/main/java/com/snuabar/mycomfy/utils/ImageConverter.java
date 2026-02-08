package com.snuabar.mycomfy.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageConverter {

    private final static String TAG = ImageConverter.class.getName();

    /**
     * 检测是否为 HEIC/HEIF 格式
     */
    public static boolean isHeicFormat(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            return mimeType != null &&
                    (mimeType.equalsIgnoreCase("image/heic") ||
                            mimeType.equalsIgnoreCase("image/heif") ||
                            mimeType.equalsIgnoreCase("image/heic-sequence") ||
                            mimeType.equalsIgnoreCase("image/heif-sequence"));
        } catch (Exception e) {
            // 通过文件扩展名检测
            String path = uri.getPath();
            if (path != null) {
                String extension = getFileExtension(path).toLowerCase();
                return extension.equals("heic") ||
                        extension.equals("heif") ||
                        extension.equals("hif");
            }
            return false;
        }
    }

    /**
     * 将 HEIC/HEIF 转换为 PNG
     * @param context Context
     * @param heicUri HEIC 文件的 Uri
     * @param outputFile 输出文件
     */
    public static void convertHeicToPng(Context context, Uri heicUri, File outputFile) {
        if (!isHeicFormat(context, heicUri)) {
            Log.i("ImageConverter", "不是 HEIC 格式，无需转换");
            return;
        }

        Bitmap bitmap = null;
        FileOutputStream fos = null;

        try {
            // 1. 解码 HEIC 图像
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), heicUri);
            bitmap = ImageDecoder.decodeBitmap(source);

            // 3. 保存为 PNG
            fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();

            Log.i("ImageConverter", "HEIC 转换成功: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("ImageConverter", "转换 HEIC 失败", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "convertHeicToPng. Error thrown. ", e);
                }
            }
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    // 辅助方法
    private static String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        return "";
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        String displayName = null;

        try {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            android.database.Cursor cursor = context.getContentResolver().query(
                    uri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                displayName = cursor.getString(nameIndex);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getFileNameFromUri. Error thrown. ", e);
        }

        if (displayName == null) {
            displayName = uri.getLastPathSegment();
        }

        return displayName;
    }
}