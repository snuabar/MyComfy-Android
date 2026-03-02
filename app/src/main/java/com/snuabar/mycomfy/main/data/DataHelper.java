package com.snuabar.mycomfy.main.data;


import static com.snuabar.mycomfy.utils.FileOperator.moveDocumentFile;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.common.FileType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class DataHelper {

    public final static String TAG = DataHelper.class.getName();
    /**
     * 操作完成
     */
    public final static int STATE_OK = 0;
    /**
     * 操作失败，没有源数据。
     */
    public final static int STATE_NO_SOURCE_DATA = 1;
    /**
     * 操作失败，没有目标目录
     */
    public final static int STATE_NO_DESTINATION = 2;
    /**
     * 操作失败，已是最新
     */
    public final static int STATE_ALREADY_LATEST = 3;
    /**
     * 操作失败，未知
     */
    public final static int STATE_UNKNOWN = 4;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private boolean isBackupOrRestoreInProgress = false;
    private final Context context;
    private final Handler handler;

    public DataHelper(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    //region 导入&导出
    public void executeExport(String destFileName, Uri destDirUri, Callbacks.CallbackT<Integer> callback) {
        if (isBackupOrRestoreInProgress) {
            return;
        }
        DocumentFile destDir = DocumentFile.fromTreeUri(context, destDirUri);
        if (destDir == null || !destDir.isDirectory() || !destDir.exists()) {
            callback.apply(STATE_NO_DESTINATION);
            return;
        }

        isBackupOrRestoreInProgress = true;
        executor.execute(() -> {
            int result = STATE_UNKNOWN;
            try {
                File zipFile = zipData(destFileName);
                if (zipFile == null || !zipFile.exists()) {
                    result = STATE_NO_SOURCE_DATA;
                } else {
                    DocumentFile zipDocumentFile = DocumentFile.fromFile(zipFile);
                    if (!moveDocumentFile(context, zipDocumentFile, destDir)) {
                        result = STATE_NO_DESTINATION;
                    } else {
                        result = STATE_OK;
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "Error during data export.", e);
            }
            final int finalResult = result;
            handler.post(() -> callback.apply(finalResult));
            isBackupOrRestoreInProgress = false;
        });
    }

    public void executeImport(Uri fileUri, Callbacks.CallbackT<Boolean> callback) {
        if (isBackupOrRestoreInProgress) {
            return;
        }

        File destDir = context.getExternalFilesDir(null);
        if (destDir == null || !destDir.isDirectory() || !destDir.exists()) {
            callback.apply(false);
            return;
        }

        isBackupOrRestoreInProgress = true;
        executor.execute(() -> {
            boolean result = unzipFileToDir(fileUri, destDir, true);
            handler.post(() -> callback.apply(result));
            isBackupOrRestoreInProgress = false;
        });
    }

    //endregion

    public String getDefaultBackupName() {
        return "MyComfy_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date.from(Instant.now(Clock.systemUTC())));
    }

    private File zipData() {
        return zipData(null);
    }

    private File zipData(String fileName) {
        File sourceDir = context.getExternalFilesDir(null);
        // 创建临时目录
        File tempDir = new File(context.getExternalCacheDir(), "temp");

        if (!tempDir.exists() && !tempDir.mkdirs()) {
            Log.e(TAG, "Failed to create temp dir.");
            return null;
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = getDefaultBackupName();
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(FileType.EXT_ZIP)) {
            fileName += FileType.EXT_ZIP;
        }
        File zipFile = new File(tempDir, fileName);
        zipExternalFilesDir(sourceDir, zipFile);
        return zipFile;
    }

    /**
     * 压缩指定目录到ZIP文件（排除temp目录）
     *
     * @param sourceDir 原目录
     * @param zipFile   生成的ZIP
     */
    public void zipExternalFilesDir(File sourceDir, File zipFile) {
        if (sourceDir == null || !sourceDir.exists()) {
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            // 压缩时排除temp目录本身
            long length = zipDirectory(sourceDir, sourceDir, zos);

            zos.close();
            fos.close();

            if (length == 0 && !zipFile.delete()) {
                Log.e(TAG, "failed to delete empty zip.");
            }
        } catch (IOException e) {
            Log.e(TAG, "zipExternalFilesDir thrown an exception:", e);
        }
    }

    /**
     * 递归压缩目录
     *
     * @param rootDir    根目录（用于计算相对路径）
     * @param currentDir 当前处理的目录
     * @param zos        ZIP输出流
     * @return total file length
     */
    private long zipDirectory(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null) return 0;

        long length = 0;

        for (File file : files) {
            // 计算相对路径
            String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();

            if (file.isDirectory()) {
                // 添加目录条目
                ZipEntry entry = new ZipEntry(relativePath + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();

                // 递归处理子目录
                length += zipDirectory(rootDir, file, zos);
            } else {
                // 添加文件条目
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                    length += len;
                }
                fis.close();
                zos.closeEntry();
            }
        }

        return length;
    }

    public boolean unzipFileToDir(Uri zipUri, File destinationDir, boolean overwrite) {
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            Log.e(TAG, "Failed to create destination directory");
            return false;
        }

        InputStream inputStream = null;
        ZipInputStream zipInputStream = null;

        try {
            // 获取输入流
            inputStream = context.getContentResolver().openInputStream(zipUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream from Uri");
                return false;
            }

            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            // 遍历zip文件中的所有条目
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 防止路径遍历攻击
                if (entryName.contains("..")) {
                    Log.w(TAG, "Skipping potentially malicious entry: " + entryName);
                    continue;
                }

                entryName = Common.correctPackageLikeStringsForDebug(entryName);
                File outputFile = new File(destinationDir, entryName);

                if (entry.isDirectory()) {
                    // 如果是目录，创建目录
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        Log.e(TAG, "Failed to create directory: " + outputFile.getAbsolutePath());
                    }
                } else {
                    if (overwrite || !outputFile.exists()) {
                        // 如果是文件，创建父目录
                        File parentDir = outputFile.getParentFile();
                        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                            Log.e(TAG, "Failed to create parent directory: " + parentDir.getAbsolutePath());
                            continue;
                        }

                        // 写入文件
                        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))) {
                            int length;
                            while ((length = zipInputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                        }
                        Log.d(TAG, "Extracted: " + outputFile.getAbsolutePath());
                    } else {
                        Log.d(TAG, "Skip: " + outputFile.getAbsolutePath());
                    }
                }
                zipInputStream.closeEntry();
            }

            Log.i(TAG, "Zip extraction completed successfully");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error during zip extraction", e);
            return false;
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

}
