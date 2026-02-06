package com.snuabar.mycomfy.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.snuabar.mycomfy.R
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.core.content.withStyledAttributes
import com.snuabar.mycomfy.common.Common
import java.time.Clock

@SuppressLint("ClickableViewAccessibility")
class AutoHideDraggableFAB @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.floatingActionButtonStyle
) : FloatingActionButton(context, attrs, defStyleAttr) {

    // 配置参数
    private var idleTimeout = 3000L // 默认3秒
    private var hideAlpha = 0.3f // 隐藏时的透明度
    private var isDraggable = true // 是否可拖动
    private var draggableArea: Rect? = null // 可拖动区域限制
    private var onPositionChanged: ((x: Float, y: Float) -> Void?)? = null

    // 状态管理
    private var isIdle = false
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null
    private var lastTouchTime = System.currentTimeMillis()

    // 拖动相关
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var dX = 0f
    private var dY = 0f
    private var parentWidth = 0
    private var parentHeight = 0

    init {
        setupFromAttributes(attrs)
        setupTouchListener()
        resetIdleTimer()

        // 添加布局监听器以确保获取正确的父容器尺寸
        addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            // 父容器尺寸发生变化时更新
            updateParentDimensions()
        }
    }

    private fun setupFromAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.withStyledAttributes(
                it,
                R.styleable.AutoHideDraggableFAB,
                0,
                0
            ) {

                idleTimeout = getInt(
                    R.styleable.AutoHideDraggableFAB_idleTimeout,
                    3000
                ).toLong()

                hideAlpha = getFloat(
                    R.styleable.AutoHideDraggableFAB_hideAlpha,
                    0.3f
                )

                isDraggable = getBoolean(
                    R.styleable.AutoHideDraggableFAB_draggable,
                    true
                )

            }
        }

        // 设置初始透明度
        alpha = 1f
    }

    private var pressedTime: Long = 0

    private fun setupTouchListener() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressedTime = Clock.systemUTC().millis()
                    if (isDraggable) {
                        // 开始拖动准备
                        isDragging = false
                        initialX = event.rawX
                        initialY = event.rawY
                        dX = x - event.rawX
                        dY = y - event.rawY

                        // 确保父容器尺寸已更新
                        updateParentDimensions()
                    }
                    // 重置空闲计时器
                    resetIdleTimer()
                    show()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDraggable) {
                        val movedDistance = sqrt(
                            (event.rawX - initialX).pow(2) +
                                    (event.rawY - initialY).pow(2)
                        )

                        // 如果移动距离超过阈值，开始拖动
                        if (movedDistance > 10) {
                            isDragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)

                            // 计算新位置
                            var newX = event.rawX + dX
                            var newY = event.rawY + dY

                            // 限制在父容器内（使用延迟计算的尺寸）
                            val minX = 0f
                            val maxX = (parentWidth - width).coerceAtLeast(0).toFloat()
                            val minY = 0f
                            val maxY = (parentHeight - height).coerceAtLeast(0).toFloat()

                            newX = newX.coerceIn(minX, maxX)
                            newY = newY.coerceIn(minY, maxY)

                            // 限制在指定区域内
                            draggableArea?.let { area ->
                                newX = newX.coerceIn(
                                    area.left.toFloat(),
                                    (area.right - width).coerceAtLeast(area.left).toFloat()
                                )
                                newY = newY.coerceIn(
                                    area.top.toFloat(),
                                    (area.bottom - height).coerceAtLeast(area.top).toFloat()
                                )
                            }

                            // 更新位置
                            x = newX
                            y = newY

                            // 回调位置变化
                            onPositionChanged?.invoke(x, y)
                            return@setOnTouchListener true
                        }
                    }
                    false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        isDragging = false
                        // 吸边效果
                        snapToEdge()
                        // 拖动结束后启动空闲计时
                        resetIdleTimer()
                        return@setOnTouchListener true
                    } else {
                        if (Clock.systemUTC().millis() - pressedTime > 500) {
                            if (isLongClickable) {
                                performLongClick()
                            }
                        } else {
                            // 如果不是拖动，执行点击
                            performClick()
                        }
                    }
                    false
                }
                else -> false
            }
        }
    }

    /**
     * 更新父容器尺寸
     */
    private fun updateParentDimensions() {
        (parent as? View)?.let { parentView ->
            // 确保父容器已经完成测量
            if (parentView.width > 0 && parentView.height > 0) {
                parentWidth = parentView.width
                parentHeight = parentView.height
            } else {
                // 如果父容器尺寸为0，使用 post 延迟获取
                parentView.post {
                    if (parentView.width > 0 && parentView.height > 0) {
                        parentWidth = parentView.width
                        parentHeight = parentView.height
                    }
                }
            }
        }
    }

    /**
     * 自动吸边到最近边缘
     */
    private fun snapToEdge() {
        // 确保父容器尺寸已更新
        if (parentWidth == 0 || parentHeight == 0) {
            updateParentDimensions()
            // 延迟执行吸边
            postDelayed({ snapToEdge() }, 50)
            return
        }

        val centerX = x + width / 2
        val centerY = y + height / 2

        // 判断离哪边更近
        val isCloserToLeft = centerX < parentWidth / 2
        val isCloserToTop = centerY < parentHeight / 2

        // 目标位置
        val margin = dipToPx(16)
        val targetX = if (isCloserToLeft) {
            margin.toFloat()
        } else {
            (parentWidth - width - margin).toFloat()
        }

        val targetY = if (isCloserToTop) {
            val statusBarHeight = Common.getStatusBarHeight(context)
            (statusBarHeight + margin).toFloat()
        } else {
            val navBarHeight = Common.getNavigationBarHeight(context)
            (parentHeight - height - navBarHeight - margin).toFloat()
        }

        // 确保目标位置在有效范围内
        val safeTargetX = targetX.coerceIn(0f, (parentWidth - width).toFloat())
        val safeTargetY = targetY.coerceIn(0f, (parentHeight - height).toFloat())

        // 动画移动到目标位置
        animate()
            .x(safeTargetX)
            .y(safeTargetY)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // 移动完成后回调
                onPositionChanged?.invoke(x, y)
            }
            .start()
    }

    /**
     * 进入空闲状态（半透明）
     */
    fun enterIdleState() {
        if (isIdle) return

        isIdle = true
        animate()
            .alpha(hideAlpha)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * 从空闲状态恢复
     */
    override fun show() {
        if (!isIdle) return

        isIdle = false
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * 重置空闲计时器
     */
    fun resetIdleTimer() {
        handler.removeCallbacksAndMessages(null)
        hideRunnable = Runnable {
            enterIdleState()
        }
        handler.postDelayed(hideRunnable!!, idleTimeout)
        lastTouchTime = System.currentTimeMillis()
    }

    /**
     * 设置空闲时间
     */
    fun setIdleTimeout(timeout: Long) {
        this.idleTimeout = timeout
        resetIdleTimer()
    }

    /**
     * 设置隐藏时的透明度
     */
    fun setHideAlpha(alpha: Float) {
        this.hideAlpha = alpha.coerceIn(0.1f, 1f)
    }

    /**
     * 设置是否可拖动
     */
    fun setDraggable(draggable: Boolean) {
        this.isDraggable = draggable
    }

    /**
     * 设置可拖动区域（相对于父容器）
     */
    fun setDraggableArea(left: Int, top: Int, right: Int, bottom: Int) {
        this.draggableArea = Rect(left, top, right, bottom)
    }

    /**
     * 设置位置变化监听器
     */
    fun setOnPositionChangedListener(listener: (x: Float, y: Float) -> Void?) {
        this.onPositionChanged = listener
    }

    /**
     * 移动到指定位置（相对于父容器）
     */
    fun moveTo(x: Float, y: Float, animate: Boolean = true) {
        // 确保位置在有效范围内
        val safeX = x.coerceIn(0f, (parentWidth - width).toFloat())
        val safeY = y.coerceIn(0f, (parentHeight - height).toFloat())

        if (animate) {
            this.animate()
                .x(safeX)
                .y(safeY)
                .setDuration(300)
                .start()
        } else {
            this.x = safeX
            this.y = safeY
        }
        onPositionChanged?.invoke(safeX, safeY)
    }

    /**
     * 获取当前位置（相对于父容器）
     */
    fun getPosition(): Pair<Float, Float> = Pair(x, y)

    /**
     * 获取相对于屏幕的位置
     */
    fun getScreenPosition(): Pair<Int, Int> {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return Pair(location[0], location[1])
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟更新父容器尺寸，确保父容器已完成布局
        post {
            updateParentDimensions()
        }
        resetIdleTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            // 布局变化时更新父容器尺寸
            updateParentDimensions()
        }
    }

    // 工具方法
    private fun dipToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

//    private fun getStatusBarHeight(): Int {
//        var result = 0
//        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
//        if (resourceId > 0) {
//            result = resources.getDimensionPixelSize(resourceId)
//        }
//        return result
//    }
//
//    private fun getNavigationBarHeight(): Int {
//        var result = 0
//        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
//        if (resourceId > 0) {
//            result = resources.getDimensionPixelSize(resourceId)
//        }
//        return result
//    }

    /**
     * 检查点是否在视图内（用于点击测试）
     */
    private fun isPointInView(x: Float, y: Float): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + width
        val bottom = top + height

        return x >= left && x <= right && y >= top && y <= bottom
    }

    /**
     * 保存状态（用于Activity重建）
     */
    fun saveState(): Bundle {
        return Bundle().apply {
            putFloat("fab_x", x)
            putFloat("fab_y", y)
            putBoolean("fab_isIdle", isIdle)
        }
    }

    /**
     * 恢复状态
     */
    fun restoreState(state: Bundle) {
        val savedX = state.getFloat("fab_x", -1f)
        val savedY = state.getFloat("fab_y", -1f)
        val savedIsIdle = state.getBoolean("fab_isIdle", false)

        if (savedX != -1f && savedY != -1f) {
            // 使用post确保在布局完成后恢复位置
            post {
                moveTo(savedX, savedY, false)
            }
        }

        if (savedIsIdle) {
            enterIdleState()
        } else {
            show()
        }
    }
}