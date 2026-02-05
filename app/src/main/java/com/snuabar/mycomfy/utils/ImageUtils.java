package com.snuabar.mycomfy.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getName();

    private static final String THUMB_EXT = ".thumb";

    public static boolean getThumbnail(AbstractMessageModel model) {
        if (isThumbnailExists(model.getImageFile())) {
            model.setThumbnailFile(getThumbnailFile(model.getImageFile()));
            return true;
        }
        return false;
    }

    private static Executor ThumbnailExecutor = null;

    public static void makeThumbnailAsync(AbstractMessageModel model, float maxWidth, float maxHeight, final Callbacks.CallbackT<AbstractMessageModel> completion) {
        if (model.getImageFile() == null) {
            return;
        }
        if (ThumbnailExecutor == null) {
            ThumbnailExecutor = Executors.newScheduledThreadPool(10);
        }
        ThumbnailExecutor.execute(() -> {
            if (model.isVideo()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    model.setThumbnailFile(VideoUtils.INSTANCE.createAndSaveThumbnail(model.getImageFile(), (int) maxWidth, (int) maxHeight));
                } else {
                    return;
                }
            } else {
                model.setThumbnailFile(ImageUtils.createAndSaveThumbnail(model.getImageFile(), maxWidth, maxHeight));
            }
            completion.apply(model);
        });
    }

    public static File getThumbnailFile(File file) {
        return new File(file.getParent(), file.getName() + THUMB_EXT);
    }

    public static int[] getImageSize(File imageFile) {
        if (imageFile != null && imageFile.exists()) {
            // 1. 解码图片并获取原始尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;

            return new int[]{originalWidth, originalHeight};
        }

        return new int[]{0, 0};
    }
    /**
     * 等比缩放Bitmap生成缩略图
     *
     * @param originalBitmap 原始Bitmap对象
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 缩放后的缩略图Bitmap
     */
    public static Bitmap createThumbnail(Bitmap originalBitmap, int maxWidth, int maxHeight) {
        // 参数检查
        if (originalBitmap == null || originalBitmap.isRecycled()) {
            return null;
        }

        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("尺寸必须大于0");
        }

        // 获取原始尺寸
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // 如果图片已经小于指定尺寸，直接返回
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return Bitmap.createBitmap(originalBitmap);
        }

        // 计算缩放比例
        float widthRatio = (float) maxWidth / originalWidth;
        float heightRatio = (float) maxHeight / originalHeight;
        float scale = Math.min(widthRatio, heightRatio);

        // 计算目标尺寸
        int targetWidth = (int) (originalWidth * scale);
        int targetHeight = (int) (originalHeight * scale);

        // 确保最小尺寸为1
        targetWidth = Math.max(1, targetWidth);
        targetHeight = Math.max(1, targetHeight);

        // 创建缩放后的Bitmap
        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
    }

    /**
     * 简化的静态方法（只有一个最大尺寸参数）
     */
    public static Bitmap createThumbnail(Bitmap originalBitmap, int maxSize) {
        return createThumbnail(originalBitmap, maxSize, maxSize);
    }

    /**
     * 将图像文件等比缩放成指定尺寸的Bitmap，并保存缩略图
     *
     * @param imageFile 原始图像文件
     * @param maxWidth  目标最大宽度
     * @param maxHeight 目标最大高度
     * @param quality   保存质量 (0-100)
     * @return 缩略图的File对象，失败返回null
     */
    public static File createAndSaveThumbnail(File imageFile, float maxWidth, float maxHeight, int quality) {
        if (imageFile == null || !imageFile.exists()) {
            return null;
        }

        try {
            // 1. 解码图片并获取原始尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;

            if (originalWidth <= 0 || originalHeight <= 0) {
                return null;
            }

            // 2. 计算缩放比例
            float scale = Math.min(maxWidth / originalWidth, maxHeight / originalHeight);

            // 如果图片比目标尺寸小，则不放大
            scale = Math.min(scale, 1.0f);

            int targetWidth = (int) (originalWidth * scale);
            int targetHeight = (int) (originalHeight * scale);

            // 3. 计算采样率
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight);
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用更节省内存的配置

            // 4. 加载并旋转图片
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if (bitmap == null) {
                return null;
            }

            // 处理图片旋转（如果图片有Exif方向信息）
            bitmap = rotateImageIfRequired(bitmap, imageFile);

            // 5. 精确缩放到位图
            if (bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                if (scaledBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                }
            }

            // 6. 保存缩略图
            File thumbFile = getThumbnailFile(imageFile);
            saveBitmapToFile(bitmap, thumbFile, quality);

            bitmap.recycle();

            return thumbFile;

        } catch (Exception e) {
            Log.e(TAG, "createAndSaveThumbnail. exception thrown.", e);
            return null;
        }
    }

    /**
     * 计算采样率
     */
    private static int calculateInSampleSize(int originalWidth, int originalHeight,
                                             int targetWidth, int targetHeight) {
        int inSampleSize = 1;

        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            int halfHeight = originalHeight / 2;
            int halfWidth = originalWidth / 2;

            while ((halfHeight / inSampleSize) >= targetHeight
                    && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 处理图片旋转
     */
    private static Bitmap rotateImageIfRequired(Bitmap bitmap, File imageFile) throws IOException {
        ExifInterface exif;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exif = new ExifInterface(imageFile);
        } else {
            exif = new ExifInterface(imageFile.getAbsolutePath());
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "rotateImageIfRequired. exception thrown.", e);
            return bitmap;
        }
    }

    /**
     * 保存Bitmap到文件
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file, int quality) {
        if (bitmap == null) {
            return false;
        }

        FileOutputStream fos = null;
        boolean success = false;

        try {
            // 确保目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            fos = new FileOutputStream(file);

            // 根据文件扩展名选择合适的压缩格式
            String fileName = file.getName().toLowerCase();
            Bitmap.CompressFormat format;

            if (fileName.endsWith(".png") || fileName.endsWith(".png.thumb")) {
                format = Bitmap.CompressFormat.JPEG;
//                quality = 100; // PNG不支持质量设置
            } else if (fileName.endsWith(".webp") || fileName.endsWith(".webp.thumb")) {
                format = Bitmap.CompressFormat.WEBP;
            } else {
                format = Bitmap.CompressFormat.JPEG;
            }

            success = bitmap.compress(format, quality, fos);
            fos.flush();

        } catch (Exception e) {
            Log.e(TAG, "saveBitmapToFile. exception thrown.", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveBitmapToFile > fos.close(). exception thrown.", e);
                }
            }
        }

        return success;
    }

    /**
     * 简化版本，使用默认质量
     */
    public static File createAndSaveThumbnail(File imageFile, float maxWidth, float maxHeight) {
        return createAndSaveThumbnail(imageFile, maxWidth, maxHeight, 85);
    }

    /**
     * 检查缩略图是否已存在
     */
    public static boolean isThumbnailExists(File imageFile) {
        if (imageFile == null) {
            return false;
        }
        File thumbFile = getThumbnailFile(imageFile);
        return thumbFile.exists();
    }

    /**
     * 删除缩略图
     */
    public static boolean deleteThumbnail(File imageFile) {
        if (imageFile == null) {
            return false;
        }
        File thumbFile = getThumbnailFile(imageFile);
        return thumbFile.exists() && thumbFile.delete();
    }

    /**
     * 复制图像文件到剪贴板
     * @param context 上下文
     * @param imageFile 图像文件
     */
    public static void copyImageToClipboard(Context context, File imageFile) {
        try {
            ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);

            // 创建文件的 URI
            Uri imageUri = Uri.fromFile(imageFile);

            // 创建 ClipData，支持图片类型
            ClipData clipData = ClipData.newUri(
                    context.getContentResolver(),
                    "Image",
                    imageUri
            );

            // 设置剪贴板内容
            clipboard.setPrimaryClip(clipData);

            Toast.makeText(context, "图片已复制到剪贴板", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "复制失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "copyImageToClipboard > exception thrown.", e);
        }
    }

    /**
     * 使用 ContentProvider 的 URI（更安全，支持 Android 10+）
     */
    public static void copyImageUsingContentUri(Context context, File imageFile) {
        try {
            ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);

            // 使用 FileProvider 获取安全的 URI（Android 7.0+ 必须）
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext().getPackageName() + ".provider",
                    imageFile
            );

            // 授予临时读取权限
            context.grantUriPermission(
                    "com.android.providers.media", // 媒体提供者的包名
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            // 创建 ClipData
            ClipData clipData = ClipData.newUri(
                    context.getContentResolver(),
                    "Image",
                    imageUri
            );

            // 添加 Intent 用于分享（可选，增强兼容性）
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

            clipData.addItem(new ClipData.Item(shareIntent));

            clipboard.setPrimaryClip(clipData);

            Toast.makeText(context, "图片已复制到剪贴板", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "copyImageUsingContentUri > exception thrown.", e);
        }
    }
}
