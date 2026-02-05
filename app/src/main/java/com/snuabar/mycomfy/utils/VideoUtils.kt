package com.snuabar.mycomfy.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.IOException

object VideoUtils {

    data class VideoSize(val width: Int, val height: Int)

    fun getVideoSize(file: File): VideoSize {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)

            // 获取视频宽度
            val widthStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )
            // 获取视频高度
            val heightStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )
            // 获取旋转角度（有些视频元数据包含旋转信息）
            val rotationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )

            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // 如果有旋转，可能需要交换宽高
            if (rotation == 90 || rotation == 270) {
                VideoSize(height, width)
            } else {
                VideoSize(width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            VideoSize(0, 0)
        } finally {
            retriever.release()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createAndSaveThumbnail(videoFile: File, maxWidth: Int, maxHeight: Int): File? {
        var bitmap: Bitmap
        try {
            val cancellationSignal = CancellationSignal()
            bitmap = ThumbnailUtils.createVideoThumbnail(
                videoFile,
                Size(maxWidth, maxHeight),
                cancellationSignal
            )
            val scaleBitmap = ImageUtils.createThumbnail(bitmap, maxWidth, maxHeight);
            bitmap.recycle()
            bitmap = scaleBitmap

        } catch (e: IOException) {
            Log.e("",
                "makeThumbnailAsync -> createVideoThumbnail -> exception thrown.",
                e
            )
            bitmap = createBitmap(maxWidth, maxHeight, Bitmap.Config.RGB_565)
        }

        // 6. 保存缩略图
        val thumbFile = ImageUtils.getThumbnailFile(videoFile)
        ImageUtils.saveBitmapToFile(bitmap, thumbFile, 85)

        return thumbFile
    }
}