package com.snuabar.mycomfy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.LruCache
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.get

/**
 * 缩略图缓存管理类
 * 支持内存缓存、异步加载、内存自动管理和缓存重建
 */
class ThumbnailCacheManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: ThumbnailCacheManager? = null

        fun init(context: Context) {
            if (instance == null) {
                instance = ThumbnailCacheManager(context.applicationContext)
            }
        }

        /**
         * 获取单例实例
         */
        fun getInstance(): ThumbnailCacheManager {
            return instance!!
        }
    }

    // 内存缓存 - 使用Bitmap缓存以提高性能
    private val memoryCache: LruCache<String, Bitmap>

    // 异步加载队列
    private val asyncHandler: Handler
    private val mainHandler = Handler(Looper.getMainLooper())

    // 加载状态跟踪
    private val loadingMap = ConcurrentHashMap<String, MutableList<LoadingObject>>()

    // 初始化上下文引用
    private val contextRef: WeakReference<Context> = WeakReference(context)

    private fun getContext(): Context? {
        return contextRef.get();
    }

    init {
        // 计算最大内存使用量（可用内存的1/8）
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        // 初始化LRU缓存
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // 返回bitmap占用的字节数（KB为单位）
                return bitmap.byteCount / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                // 当Bitmap被移除时，可以在这里进行清理操作
                if (evicted) {
                    // 如果需要保存到磁盘缓存，可以在这里实现
                }
            }
        }

        // 初始化异步处理线程
        val handlerThread = HandlerThread("ThumbnailLoader")
        handlerThread.start()
        asyncHandler = Handler(handlerThread.looper)
    }

    fun getThumbnail(thumbnailPath: String): Bitmap? {
        // 检查输入参数
        if (thumbnailPath.isEmpty()) {
            return null
        }

        // 从内存缓存获取
        val cachedBitmap = memoryCache.get(thumbnailPath)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            return cachedBitmap
        }
        return null;
    }

    fun getThumbnailAsync(
        thumbnailPath: String,
        passBack: Any? = null,
        callback: LoadCallback? = null
    ): Boolean {
        return getThumbnailAsync(thumbnailPath, passBack, callback, false)
    }

    /**
     * 获取缩略图的主要接口
     * @param thumbnailPath 缩略图文件路径
     * @param callback 加载回调
     * @param forceRefresh 是否强制刷新缓存（重新读取文件）
     * @return 如果缓存中存在，返回true；否则返回false并异步加载
     */
    fun getThumbnailAsync(
        thumbnailPath: String,
        passBack: Any? = null,
        callback: LoadCallback? = null,
        forceRefresh: Boolean = false
    ): Boolean {
        // 检查输入参数
        if (thumbnailPath.isEmpty()) {
            callback?.onBitmapLoaded(null, passBack)
            return false
        }

        // 从内存缓存获取
        if (!forceRefresh) {
            val cachedBitmap = memoryCache.get(thumbnailPath)
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                callback?.onBitmapLoaded(cachedBitmap, passBack)
                return true
            }
        } else {
            // 强制刷新时移除旧缓存
            memoryCache.remove(thumbnailPath)
        }

        // 检查是否已经在加载中
        synchronized(loadingMap) {
            val callbacks = loadingMap[thumbnailPath]
            if (callbacks != null) {
                // 已经在加载中，添加回调到等待列表
                if (callback != null) {
                    callbacks.add(LoadingObject(passBack, callback))
                }
                return false
            } else {
                // 新建加载队列
                val newCallbacks = mutableListOf<LoadingObject>()
                if (callback != null) {
                    newCallbacks.add(LoadingObject(passBack, callback))
                }
                loadingMap[thumbnailPath] = newCallbacks
            }
        }

        // 异步加载
        asyncHandler.post {
            loadBitmapFromFile(thumbnailPath)
        }

        return false
    }

    /**
     * 从文件加载Bitmap
     */
    private fun loadBitmapFromFile(thumbnailPath: String) {
        val file = File(thumbnailPath)
        var bitmap: Bitmap? = null

        if (file.exists() && file.length() > 0) {
            try {
                // 读取文件并解码为Bitmap
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565 // 使用更节省内存的配置
                    inSampleSize = 1
                }

                FileInputStream(file).use { fis ->
                    bitmap = BitmapFactory.decodeStream(fis, null, options)
                }

//                // 检查是否加载了无效的黑色缩略图
//                if (bitmap != null && isBlackThumbnail(bitmap!!)) {
//                    bitmap?.recycle()
//                    bitmap = null
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 处理加载结果
        handleLoadResult(thumbnailPath, bitmap)
    }

    /**
     * 处理加载结果并通知所有等待的回调
     */
    private fun handleLoadResult(thumbnailPath: String, bitmap: Bitmap?) {
        val loadingObjects: MutableList<LoadingObject>?

        synchronized(loadingMap) {
            loadingObjects = loadingMap.remove(thumbnailPath)
        }

        // 添加到内存缓存
        if (bitmap != null && !bitmap.isRecycled) {
            memoryCache.put(thumbnailPath, bitmap)
        }

        // 在主线程通知所有回调
        if (loadingObjects != null && loadingObjects.isNotEmpty()) {
            mainHandler.post {
                for (`object` in loadingObjects) {
                    `object`.callback.onBitmapLoaded(bitmap, `object`.passBack)
                }
            }
        }
    }

    /**
     * 检查是否是黑色的缩略图
     */
    private fun isBlackThumbnail(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 0 || bitmap.height <= 0) return true

        // 简单的检查：取几个采样点检查是否都是黑色
        val pixels = IntArray(9)
        val width = bitmap.width
        val height = bitmap.height

        // 采样9个点（3x3网格）
        var index = 0
        for (i in 1..3) {
            for (j in 1..3) {
                val x = width * i / 4
                val y = height * j / 4
                if (x < width && y < height) {
                    pixels[index++] = bitmap[x, y]
                }
            }
        }

        // 检查所有采样点是否接近黑色
        val blackThreshold = 0x0A0A0A // 非常接近黑色的阈值
        for (i in 0 until index) {
            val color = pixels[i] and 0x00FFFFFF // 忽略Alpha通道
            if (color > blackThreshold) {
                return false
            }
        }

        return true
    }

    /**
     * 清除指定缩略图的缓存（用于重建）
     */
    fun clearCacheForPath(thumbnailPath: String) {
        // 从内存缓存移除
        val bitmap = memoryCache.remove(thumbnailPath)

        // 回收Bitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }

        // 取消正在进行的加载
        synchronized(loadingMap) {
            loadingMap.remove(thumbnailPath)?.clear()
        }

        // 注意：这里不删除文件，文件删除由外部逻辑处理
    }

    /**
     * 重建指定缩略图的缓存
     * @param thumbnailPath 缩略图文件路径
     * @param callback 重建完成回调
     */
    fun rebuildThumbnailCache(thumbnailPath: String, callback: LoadCallback? = null) {
        // 先清除缓存
        clearCacheForPath(thumbnailPath)

        // 重新加载（强制刷新）
        getThumbnailAsync(thumbnailPath, callback, forceRefresh = true)
    }

    /**
     * 手动释放资源
     */
    fun release() {
        // 清空内存缓存
        memoryCache.evictAll()

        // 清空加载队列
        loadingMap.clear()

        // 注意：不清理文件，文件由外部管理
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            hitCount = memoryCache.hitCount(),
            missCount = memoryCache.missCount(),
            evictionCount = memoryCache.evictionCount(),
            currentSize = memoryCache.size(),
            maxSize = memoryCache.maxSize(),
            loadingCount = loadingMap.size
        )
    }

    /**
     * 加载回调接口
     */
    interface LoadCallback {
        fun onBitmapLoaded(bitmap: Bitmap?, passBack: Any?)
    }

    data class LoadingObject(
        val passBack: Any?,
        val callback: LoadCallback
    )

    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val hitCount: Int,
        val missCount: Int,
        val evictionCount: Int,
        val currentSize: Int,
        val maxSize: Int,
        val loadingCount: Int
    )
}