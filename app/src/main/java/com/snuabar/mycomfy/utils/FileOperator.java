package com.snuabar.mycomfy.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import android.content.Intent;
import android.net.Uri;

import java.io.File;


public class FileOperator {

    private static final String Tag = FileOperator.class.getName();

    /**
     * 移动DocumentFile文件到目标目录
     *
     * @param context      上下文
     * @param sourceFile   要移动的源文件
     * @param targetFolder 目标文件夹
     * @return 移动成功返回true
     */
    public static boolean moveDocumentFile(Context context, DocumentFile sourceFile, DocumentFile targetFolder) {
        return copyDocumentFile(context, sourceFile, targetFolder, true);
    }

    /**
     * 复制DocumentFile文件到目标目录
     *
     * @param context      上下文
     * @param sourceFile   要移动的源文件
     * @param targetFolder 目标文件夹
     * @param deleteSource 是否删除源文件
     * @return 移动成功返回true
     */
    public static boolean copyDocumentFile(Context context, DocumentFile sourceFile, DocumentFile targetFolder, boolean deleteSource) {
        if (sourceFile == null || !sourceFile.exists() ||
                targetFolder == null || !targetFolder.isDirectory()) {
            return false;
        }

        // 1. 在目标位置创建新文件
        DocumentFile newFile = targetFolder.createFile(
                Objects.requireNonNull(sourceFile.getType()),
                Objects.requireNonNull(sourceFile.getName()));

        if (newFile == null) return false;

        // 复制内容
        if (!copyFileContent(context, sourceFile, newFile)) {
            newFile.delete(); // 复制失败则删除已创建的文件
            return false;
        }

        if (deleteSource) {
            // 3. 删除原文件
            return sourceFile.delete();
        }
        return true;
    }

    /**
     * 复制文件内容
     */
    private static boolean copyFileContent(Context context,
                                    DocumentFile source,
                                    DocumentFile target) {
        try (InputStream in = context.getContentResolver().openInputStream(source.getUri());
             OutputStream out = context.getContentResolver().openOutputStream(target.getUri())) {

            if (in == null || out == null) {
                Log.e(Tag, "Failed to copy file content. Stream is null.");
                return false;
            }

            byte[] buffer = new byte[1024 * 4]; // 4KB缓冲区
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (IOException e) {
            Log.e(Tag, "Failed to copy file content.", e);
            return false;
        }
        return true;
    }

    public static void shareImageFromLocal(Context context, File imageFile) {
        // 2. 检查文件是否存在
        if (!imageFile.exists()) {
            Log.e("ShareImage", "Image file does not exist: " + imageFile.getName());
            return;
        }

        // 3. 获取文件的Uri（适配不同Android版本）
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0及以上需要使用FileProvider
            imageUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider",
                    imageFile);
        } else {
            // Android 7.0以下
            imageUri = Uri.fromFile(imageFile);
        }

        // 4. 创建分享Intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*"); // 设置分享类型为图片
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 授予读取权限

        // 5. 可选：添加分享标题
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享图片");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "看看这张有趣的图片！");

        // 6. 启动分享选择器
        context.startActivity(Intent.createChooser(shareIntent, "分享到"));
    }
}
