package com.snuabar.mycomfy.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.net.toUri
import com.snuabar.mycomfy.R
import com.snuabar.mycomfy.utils.ViewUtils
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.schedule

class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 视图组件
    private lateinit var videoSurface: android.widget.VideoView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var controlsLayout: LinearLayout

    // 控制是否显示控制面板
    private var controlsVisible = true
    private var hideControlsTimer: Timer? = null

    // 播放状态
    private var isPlaying = false
    private var isPrepared = false

    init {
        initView(context)
        initMediaPlayer()
        setupClickListeners()
        setupSeekBar()
    }

    private fun initView(context: Context) {
        // 使用FrameLayout作为根布局
        LayoutInflater.from(context).inflate(R.layout.video_player_layout, this, true)

        // 初始化视图组件
        videoSurface = findViewById(R.id.videoSurface)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        controlsLayout = findViewById(R.id.controlsLayout)

        // 设置初始UI状态
        updatePlayButton()
    }

    private fun initMediaPlayer() {
        // 设置VideoView回调
        videoSurface.setOnPreparedListener {
            isPrepared = true
            updateTotalTime()
            startProgressUpdate()
        }

        videoSurface.setOnCompletionListener {
            isPlaying = false
            updatePlayButton()
            seekBar.progress = seekBar.max
            stopProgressUpdate()
        }

        videoSurface.setOnErrorListener { _, what, extra ->
            // 处理播放错误
            false
        }
    }

    private fun setupClickListeners() {
        // 播放/暂停按钮点击事件
        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

//        // 视频区域点击事件（显示/隐藏控制面板）
//        videoSurface.setOnClickListener {
//            toggleControls()
//        }

        // 控制面板点击事件（防止事件传递到视频区域）
        controlsLayout.setOnClickListener {
            // 阻止点击事件传递到视频区域
        }
    }

    fun getControlLayoutHeight(): Int {
        ViewUtils.measure(controlsLayout, 0)
        return controlsLayout.height
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && isPrepared) {
                    val duration = videoSurface.duration
                    val newPosition = (duration * progress / 100).toInt()
                    tvCurrentTime.text = formatTime(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 开始拖动时暂停进度更新
                stopProgressUpdate()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (isPrepared) {
                    val duration = videoSurface.duration
                    val newPosition = (duration * seekBar.progress / 100).toInt()
                    videoSurface.seekTo(newPosition)

                    // 恢复进度更新
                    startProgressUpdate()

                    // 如果正在播放，继续播放
                    if (isPlaying) {
                        videoSurface.start()
                    }
                }
            }
        })
    }

    /**
     * 设置视频源
     * @param videoPath 视频文件路径或URI
     */
    fun setVideoSource(videoPath: String) {
        release();

        if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            videoSurface.setVideoURI(videoPath.toUri())
        } else {
            videoSurface.setVideoPath(videoPath)
        }

        play()
    }

    fun setVideoUri(uri: Uri) {
        videoSurface.setVideoURI(uri)
    }

    /**
     * 切换播放/暂停状态
     */
    fun togglePlayPause() {
        if (!isPrepared) {
            videoSurface.start()
            isPrepared = true
        }

        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * 开始播放
     */
    fun play() {
        if (!isPlaying) {
            videoSurface.start()
            isPlaying = true
            updatePlayButton()
            startProgressUpdate()
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (isPlaying) {
            videoSurface.pause()
            isPlaying = false
            updatePlayButton()
            stopProgressUpdate()
        }
    }

    /**
     * 跳转到指定位置
     * @param position 位置（毫秒）
     */
    fun seekTo(position: Int) {
        if (isPrepared) {
            videoSurface.seekTo(position)
        }
    }

    /**
     * 切换控制面板显示/隐藏
     */
    fun toggleControls() {
        controlsVisible = !controlsVisible
        if (controlsVisible) {
            controlsLayout.animate().alpha(1.0f).withStartAction { controlsLayout.visibility = VISIBLE }
        } else {
            controlsLayout.animate().alpha(0.0f).withEndAction { controlsLayout.visibility = GONE }
        }
    }

    /**
     * 开始更新播放进度
     */
    private fun startProgressUpdate() {
        stopProgressUpdate()

        // 使用定时器更新进度
        hideControlsTimer = Timer()
        hideControlsTimer?.schedule(0, 1000) {
            post {
                if (isPrepared && isPlaying) {
                    val currentPosition = videoSurface.currentPosition
                    val duration = videoSurface.duration

                    if (duration > 0) {
                        // 更新进度条
                        val progress = (currentPosition * 100 / duration)
                        seekBar.progress = progress

                        // 更新时间显示
                        tvCurrentTime.text = formatTime(currentPosition)

                        // 确保总时间是最新的
                        if (tvTotalTime.text == "00:00") {
                            updateTotalTime()
                        }
                    }
                }
            }
        }
    }

    /**
     * 停止更新播放进度
     */
    private fun stopProgressUpdate() {
        hideControlsTimer?.cancel()
        hideControlsTimer = null
    }

    /**
     * 更新总时间显示
     */
    private fun updateTotalTime() {
        if (isPrepared) {
            val duration = videoSurface.duration
            tvTotalTime.text = formatTime(duration)
            seekBar.max = 100
        }
    }

    /**
     * 更新播放按钮状态
     */
    private fun updatePlayButton() {
        val iconRes = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        btnPlayPause.setImageResource(iconRes)
    }

    /**
     * 格式化时间（毫秒 -> mm:ss）
     */
    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * 释放资源
     */
    fun release() {
        stopProgressUpdate()
        videoSurface.stopPlayback()
        isPrepared = false
        isPlaying = false
    }

    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Int = videoSurface.currentPosition

    /**
     * 获取视频总时长（毫秒）
     */
    fun getDuration(): Int = videoSurface.duration

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean = isPlaying

    /**
     * 是否已准备好
     */
    fun isPrepared(): Boolean = isPrepared

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}