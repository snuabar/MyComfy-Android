package com.snuabar.mycomfy.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.util.Log;

import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.common.Callbacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getName();

    private static final String THUMB_EXT = ".thumb";

    public static class ImageContent {
        public final File imageFile;
        public final File promptFile;
        private File thumbnailFile;
        private Parameters params;

        public ImageContent(File imageFile, File promptFile) {
            this.imageFile = imageFile;
            this.promptFile = promptFile;
        }

        public File getThumbnailFile() {
            return thumbnailFile;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ImageContent that = (ImageContent) o;
            return Objects.equals(imageFile, that.imageFile) && Objects.equals(promptFile, that.promptFile) && Objects.equals(thumbnailFile, that.thumbnailFile) && Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(imageFile, promptFile, thumbnailFile, params);
        }

        public Parameters getParams() {
            if (params == null) {
                if (promptFile != null && promptFile.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(Paths.get(promptFile.getAbsolutePath()));
                        String jsonString = new String(bytes, StandardCharsets.UTF_8);

                        JSONObject jsonObject = new JSONObject(jsonString);
                        params = new Parameters();
                        params.loadJson(jsonObject);
                    } catch (IOException e) {
                        Log.e(TAG, "getParams: failed to read prompt from file.");
                    } catch (JSONException e) {
                        Log.e(TAG, "getParams: failed to process json string.");
                    }
                }
            }
            return params;
        }
    }

    public static boolean getThumbnail(ImageContent imageContent) {
        if (isThumbnailExists(imageContent.imageFile)) {
            imageContent.thumbnailFile = getThumbnailFile(imageContent.imageFile);
            return true;
        }
        return false;
    }

    private static Executor ThumbnailExecutor = null;
    public static void makeThumbnailAsync(ImageContent imageContent, float maxWidth, float maxHeight, final Callbacks.CallbackT<ImageContent> completion) {
        if (ThumbnailExecutor == null) {
            ThumbnailExecutor = Executors.newScheduledThreadPool(10);
        }
        ThumbnailExecutor.execute(() -> {
            imageContent.thumbnailFile = ImageUtils.createAndSaveThumbnail(imageContent.imageFile, maxWidth, maxHeight);
            completion.apply(imageContent);
        });
    }

    private static File getThumbnailFile(File imageFile) {
        return new File(imageFile.getParent(), imageFile.getName() + THUMB_EXT);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
    private static boolean saveBitmapToFile(Bitmap bitmap, File file, int quality) {
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
                format = Bitmap.CompressFormat.PNG;
                quality = 100; // PNG不支持质量设置
            } else if (fileName.endsWith(".webp") || fileName.endsWith(".webp.thumb")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    format = Bitmap.CompressFormat.WEBP;
                } else {
                    format = Bitmap.CompressFormat.JPEG;
                }
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
}
