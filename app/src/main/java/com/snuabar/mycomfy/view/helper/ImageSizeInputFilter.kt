package com.snuabar.mycomfy.view.helper

import android.text.InputFilter
import android.text.Spanned
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.util.Size
import android.view.View
import kotlin.math.roundToInt

/**
 * 图像尺寸输入过滤器
 * @param isWidthFilter true表示宽度过滤器，false表示高度过滤器
 * @param bitmapSize 参考位图（用于图生图模式下的等比缩放）
 */
private class ImageSizeInputFilter(
    private val isWidthFilter: Boolean
) : InputFilter {

    companion object {
        const val MAX_SIZE = 4096 // 最大尺寸限制
        const val MIN_SIZE = 0   // 最小尺寸限制
        private const val MULTIPLE = 8    // 必须是8的倍数
    }

    var bitmapSize: Size? = null

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {

        // 构建最终的输入字符串
        val newText = buildString {
            if (dest != null && dstart > 0) {
                append(dest.subSequence(0, dstart))
            }
            if (source != null) {
                append(source.subSequence(start, end))
            }
            if (dest != null && dend < dest.length) {
                append(dest.subSequence(dend, dest.length))
            }
        }

        // 允许空输入
        if (newText.isEmpty()) {
            return null
        }

        // 检查是否为有效的数字
        val number = newText.toIntOrNull() ?: return "" // 拒绝非数字输入

        // 检查数字范围
        if (number !in MIN_SIZE..MAX_SIZE) {
            return ""
        }

        return null // 允许输入
    }

    /**
     * 将输入的尺寸调整为8的倍数
     */
    fun adjustToMultiple(value: Int): Int {
        // 四舍五入到最接近的8的倍数
        return ((value + MULTIPLE / 2) / MULTIPLE) * MULTIPLE
            .coerceIn(MIN_SIZE, MAX_SIZE)
    }

    /**
     * 根据参考位图计算等比缩放尺寸
     * @param currentValue 当前输入的尺寸值
     * @param otherValue 另一个维度的当前值（用于保持比例）
     * @return 调整后的尺寸对 Pair(宽度, 高度)
     */
    fun calculateAspectRatioSize(
        currentValue: Int,
        otherValue: Int
    ): Pair<Int, Int> {
        val bitmap = bitmapSize ?: return Pair(currentValue, otherValue)

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        return if (isWidthFilter) {
            // 当前正在调整宽度，根据宽度计算高度
            val newHeight = (currentValue.toFloat() * bitmapHeight / bitmapWidth).roundToInt()
            val adjustedHeight = adjustToMultiple(newHeight)
            Pair(currentValue, adjustedHeight)
        } else {
            // 当前正在调整高度，根据高度计算宽度
            val newWidth = (currentValue.toFloat() * bitmapWidth / bitmapHeight).roundToInt()
            val adjustedWidth = adjustToMultiple(newWidth)
            Pair(adjustedWidth, currentValue)
        }
    }
}

/**
 * 尺寸输入管理器 - 用于协调两个EditText的输入
 */
class ImageSizeInputManager(
    private val widthEditText: EditText,
    private val heightEditText: EditText
) {

    private val widthFilter = ImageSizeInputFilter(true)
    private val heightFilter = ImageSizeInputFilter(false)

    // 标记是否正在自动更新，防止循环调用
    private var isUpdating = false
    var bitmapSize: Size? = null
        set(value) {
            field = value
            widthFilter.bitmapSize = bitmapSize
            heightFilter.bitmapSize = bitmapSize
        }

    init {
        // 设置输入过滤器
        widthEditText.filters = arrayOf(widthFilter)
        heightEditText.filters = arrayOf(heightFilter)

        // 设置焦点变化监听
        setupFocusChangeListeners()

        // 设置文本变化监听
        setupTextChangeListeners()
    }

    private fun setupFocusChangeListeners() {
        val focusChangeListener = View.OnFocusChangeListener { view: View, hasFocus: Boolean ->
            if (!hasFocus) {
                val editText = view as EditText
                val currentValue = editText.text.toString().toIntOrNull()

                if (currentValue != null) {
                    // 失去焦点时调整数值
                    if (bitmapSize != null) {
                        // 图生图模式：等比缩放
                        adjustWithAspectRatio(editText, currentValue)
                    } else {
                        // 文生图模式：直接调整到8的倍数
                        adjustToMultiple(editText, currentValue)
                    }
                }
            }
        }

        widthEditText.onFocusChangeListener = focusChangeListener
        heightEditText.onFocusChangeListener = focusChangeListener
    }

    private fun setupTextChangeListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || bitmapSize == null) return

                val text = s?.toString() ?: return
                val currentValue = text.toIntOrNull() ?: return

                // 图生图模式下实时计算另一边的尺寸
                isUpdating = true

                try {
                    val (newWidth, newHeight) = if (s === widthEditText.text) {
                        widthFilter.calculateAspectRatioSize(
                            currentValue,
                            heightEditText.text.toString().toIntOrNull() ?: currentValue
                        )
                    } else {
                        heightFilter.calculateAspectRatioSize(
                            currentValue,
                            widthEditText.text.toString().toIntOrNull() ?: currentValue
                        )
                    }

                    // 更新另一个输入框
                    if (s === widthEditText.text) {
                        heightEditText.setText(newHeight.toString())
                    } else {
                        widthEditText.setText(newWidth.toString())
                    }
                } finally {
                    isUpdating = false
                }
            }
        }

        widthEditText.addTextChangedListener(textWatcher)
        heightEditText.addTextChangedListener(textWatcher)
    }

    private fun adjustToMultiple(editText: EditText, value: Int) {
        val filter = if (editText === widthEditText) widthFilter else heightFilter
        val adjustedValue = filter.adjustToMultiple(value)

        if (adjustedValue != value) {
            isUpdating = true
            editText.setText(adjustedValue.toString())
            editText.setSelection(editText.text.length)
        }
        isUpdating = false
    }

    private fun adjustWithAspectRatio(editText: EditText, value: Int) {
        val otherValue = if (editText === widthEditText) {
            heightEditText.text.toString().toIntOrNull() ?: value
        } else {
            widthEditText.text.toString().toIntOrNull() ?: value
        }

        val filter = if (editText === widthEditText) widthFilter else heightFilter
        val (newWidth, newHeight) = filter.calculateAspectRatioSize(value, otherValue)

        isUpdating = true

        if (editText === widthEditText) {
            if (newWidth != value) {
                widthEditText.setText(newWidth.toString())
            }
            if (newHeight != otherValue) {
                heightEditText.setText(newHeight.toString())
            }
        } else {
            if (newHeight != value) {
                heightEditText.setText(newHeight.toString())
            }
            if (newWidth != otherValue) {
                widthEditText.setText(newWidth.toString())
            }
        }

        isUpdating = false
    }

    /**
     * 获取当前有效的尺寸（总是8的倍数）
     */
    fun getValidatedSize(): Pair<Int, Int> {
        val width = widthEditText.text.toString().toIntOrNull() ?: ImageSizeInputFilter.MIN_SIZE
        val height = heightEditText.text.toString().toIntOrNull() ?: ImageSizeInputFilter.MIN_SIZE

        return Pair(
            widthFilter.adjustToMultiple(width),
            heightFilter.adjustToMultiple(height)
        )
    }
}