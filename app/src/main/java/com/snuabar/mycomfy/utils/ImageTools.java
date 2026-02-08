package com.snuabar.mycomfy.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ImageTools {
    private static final String TAG = ImageTools.class.getName();
    private static final int REQUEST_CAMERA_PERMISSION = 1000;
    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    private static ImageTools Instance;

    public static ImageTools getInstance() {
        if (Instance == null) {
            throw new NullPointerException("Call ImageTools.Initialize() to initialize this object.");
        }
        return Instance;
    }

    public static void Initialize(Context context) {
        if (Instance == null) {
            Instance = new ImageTools(context);
        }
    }

    private final Context context;
    private File currentPhotoFile;
    private OnPictureResultListener pictureResultListener;
    private final ActivityResultLauncher<Intent> mFileOpenLauncher;
    private final ActivityResultLauncher<Intent> mCameraLauncher;
    // 用于存储临时照片文件的Uri
    private Uri currentPhotoUri;

    public interface OnPictureResultListener {
        void onPictureResult(File file);

        void onError(String message);
    }

    ImageTools(Context context) {
        this.context = context.getApplicationContext();

        mFileOpenLauncher = ((AppCompatActivity) context).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (pictureResultListener != null &&
                    result != null && result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                onPictureSelect(result.getData().getData());
            }
        });
        mCameraLauncher = ((AppCompatActivity) context).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (currentPhotoUri != null) {
                currentPhotoFile = getFileFromUri(currentPhotoUri);
            } else {
                if (pictureResultListener != null) {
                    pictureResultListener.onError("未知错误");
                }
                return;
            }
            if (result.getResultCode() == Activity.RESULT_OK) {
                // 如果选择保存到相册，添加到系统相册
                if (pictureResultListener != null) {
                    pictureResultListener.onPictureResult(currentPhotoFile);
                }
            } else {
                // 用户取消拍照，删除临时文件
                if (currentPhotoFile != null && currentPhotoFile.exists() && !currentPhotoFile.delete()) {
                    Log.e(TAG, "Failed to delete file " + currentPhotoFile);
                }
                if (pictureResultListener != null) {
                    pictureResultListener.onError("用户已取消。");
                }
            }
        });
    }

    @NonNull
    private Context getContext() {
        return context;
    }

    /**
     * 用系统应用选择图像文件
     *
     * @param listener 拍照完成回调
     */
    public void selectPicture(OnPictureResultListener listener) {
        this.pictureResultListener = listener;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        mFileOpenLauncher.launch(intent);
    }

    private void onPictureSelect(Uri fileUri) {
        if (fileUri != null) {
            if (ImageConverter.isHeicFormat(getContext(), fileUri)) {
                currentPhotoFile = createImageFile(fileUri, ".png", false);
                if (currentPhotoFile.exists() && currentPhotoFile.length() > 0) {
                    if (pictureResultListener != null) {
                        pictureResultListener.onPictureResult(currentPhotoFile);
                    }
                    return;
                }
                ImageConverter.convertHeicToPng(getContext(), fileUri, currentPhotoFile);
                if (pictureResultListener != null) {
                    if (currentPhotoFile.exists() && currentPhotoFile.length() > 0) {
                        pictureResultListener.onPictureResult(currentPhotoFile);
                    } else {
                        pictureResultListener.onError("转换失败。");
                    }
                }
            } else {
                currentPhotoFile = createImageFile(fileUri, null, false);
                if (!currentPhotoFile.exists() || currentPhotoFile.length() == 0) {
                    // Copy the selected file to our target file
                    try {
                        // Get the input stream from the selected file
                        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                        if (inputStream != null) {
                            // Copy the content to our target file
                            OutputStream outputStream = Files.newOutputStream(currentPhotoFile.toPath());
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                            outputStream.close();
                            inputStream.close();

                            if (pictureResultListener != null) {
                                pictureResultListener.onPictureResult(currentPhotoFile);
                            }
                        } else {
                            if (pictureResultListener != null) {
                                pictureResultListener.onError("无法打开文件输入流");
                            }
                        }
                    } catch (Exception e) {
                        if (pictureResultListener != null) {
                            pictureResultListener.onError("复制文件失败: " + e.getMessage());
                        }
                    }
                } else {
                    if (pictureResultListener != null) {
                        pictureResultListener.onPictureResult(currentPhotoFile);
                    }
                }
            }
        }
    }

    /**
     * 调用系统相机拍照
     *
     * @param activity 调用此方法的Activity
     * @param listener 拍照完成回调
     */
    public void takePicture(Activity activity, OnPictureResultListener listener) {
        this.pictureResultListener = listener;

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        // Android 13以下还需要存储权限来保存照片
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        }

        dispatchTakePictureIntent(activity);
    }

    private void dispatchTakePictureIntent(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // 确保有相机应用可以处理该意图
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            // 创建 ContentValues 用于保存到 MediaStore
            ContentValues values = new ContentValues();
            String fileName = generateFileName();

            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyComfy");
//            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            // 插入到 MediaStore 并获取 Uri
            currentPhotoUri = activity.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);

            mCameraLauncher.launch(takePictureIntent);
        } else {
            if (pictureResultListener != null) {
                pictureResultListener.onError("没有可用的相机应用");
            }
        }
    }

    /**
     * 生成文件名
     */
    private static String generateFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        return "IMG_" + timeStamp + ".jpg";
    }

    /**
     * 在Activity的onRequestPermissionsResult中调用此方法
     */
    public void handleRequestPermissionsResult(Activity activity, int requestCode,
                                               @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新尝试拍照
                // We need to determine which operation was requested
                // This is a simplified approach - in practice, you might want to store
                // the operation type in a field
                dispatchTakePictureIntent(activity);
            } else {
                if (pictureResultListener != null) {
                    pictureResultListener.onError("相机权限被拒绝");
                }
            }
        }
    }

    private File getExchangeDir() {
        File file = context.getExternalFilesDir(null);
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getMessageDir: failed to execute mkdirs()");
        }
        file = context.getExternalFilesDir("exchange");
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getMessageDir: failed to execute mkdirs()");
        }
        return file;
    }

    /**
     * 创建图片文件
     */
    private File createImageFile(Uri fileUri, String extension, boolean newFile) {
        // 获取应用的私有存储目录
        File storageDir = getExchangeDir();

        if (extension != null && !extension.startsWith(".")) {
            extension = "." + extension;
        }

        // 创建图片文件
        String imageFileName = fileUri == null ? generateFileName() : DocumentFile.fromSingleUri(context, fileUri).getName();
        if (imageFileName == null) {
            imageFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        }

        if (extension != null && !imageFileName.endsWith(extension)) {
            int lastDot = imageFileName.lastIndexOf('.');
            if (lastDot != -1 && lastDot < imageFileName.length() - 1) {
                imageFileName = imageFileName.substring(0, lastDot) + extension;
            }
        }
        File imageFile = new File(storageDir, imageFileName);

        // 如果文件已存在，删除它
        if (imageFile.exists() && newFile) {
            if (!imageFile.delete()) {
                Log.e(TAG, "createImageFile -> failed to delete file " + imageFile);
            }
        }

        return imageFile;
    }

    /**
     * 从Uri获取File对象（可能为null）
     */
    private File getFileFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) {
            return new File(Objects.requireNonNull(uri.getPath()));
        }

        // 对于content:// Uri，尝试获取真实路径
        if (scheme.equals("content")) {
            String realPath = getRealPathFromUri(uri);
            if (realPath != null) {
                return new File(realPath);
            }
        }

        return null;
    }

    /**
     * 从content:// Uri获取真实路径
     */
    private String getRealPathFromUri(Uri contentUri) {
        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                    contentUri,
                    new String[]{MediaStore.Images.Media.DATA},
                    null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    return filePath;
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getRealPathFromUri -> exception thrown", e);
        }
        return null;
    }

}