package com.snuabar.mycomfy.view

// PromptEditText.kt
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import com.snuabar.mycomfy.R

class PromptEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var suggestionList: ListView? = null
    private var popupWindow: PopupWindow? = null
    private var suggestionAdapter: ArrayAdapter<String>? = null
    private var suggestions: MutableList<String> = mutableListOf()

    init {
        setupPopupWindow()
        loadDefaultSuggestions()

        // 监听文本变化
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                handleTextChanged(s?.toString() ?: "")
            }
        })
    }

    private fun setupPopupWindow() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.layout_suggestion_popup, null)

        suggestionList = popupView.findViewById(R.id.suggestion_list)
        suggestionAdapter = ArrayAdapter(context, R.layout.layout_suggestion_item, R.id.text1)
        suggestionList?.adapter = suggestionAdapter

        // 设置ListView点击事件
        suggestionList?.setOnItemClickListener { _, _, position, _ ->
            val selected = suggestionAdapter?.getItem(position) as String
            insertSuggestion(selected)
        }

        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            // 设置背景，让PopupWindow可以响应外部点击
            setBackgroundDrawable (AppCompatResources.getDrawable(
                context,
                R.drawable.parameters_popup_bg
            ))
            isOutsideTouchable = false
            elevation = 8.0f
        }
    }

    private fun loadDefaultSuggestions() {
        // AI提示词常用词汇库
        val aiPromptKeywords = listOf(
            // 质量相关
            "high quality", "masterpiece", "best quality", "ultra detailed", "4k", "8k",
            "photorealistic", "cinematic", "professional",

            // 艺术风格
            "realistic", "anime", "cartoon", "painting", "watercolor", "oil painting",
            "digital art", "concept art", "vector art", "minimalist",

            // 光照和氛围
            "dramatic lighting", "soft lighting", "golden hour", "sunset", "morning light",
            "neon lights", "volumetric lighting", "rim light", "ambient light",

            // 视角和构图
            "wide angle", "close-up", "portrait", "landscape", "bird's eye view",
            "low angle", "macro shot", "panoramic", "symmetrical",

            // 渲染引擎
            "unreal engine", "unity", "octane render", "cycles", "v-ray",
            "blender", "maya", "3ds max",

            // 特殊效果
            "bokeh", "depth of field", "motion blur", "HDR", "glow", "shadows",
            "reflections", "transparency", "particles",

            // AI模型相关
            "stable diffusion", "midjourney", "dalle", "chatgpt", "GPT-4",
            "prompt engineering", "negative prompt", "weights",

            // 常用参数
            "--ar", "--v", "--quality", "--style", "--chaos", "--stylize",
            "--no", "--iw", "--seed",

            // 艺术家和工作室
            "art by Studio Ghibli", "by Pixar", "by Van Gogh", "by Monet",
            "by James Cameron", "by Wes Anderson",

            // 主题和场景
            "fantasy", "sci-fi", "cyberpunk", "steampunk", "post-apocalyptic",
            "medieval", "futuristic", "retro", "vintage"
        )

        suggestions.addAll(aiPromptKeywords.sorted())
    }

    private fun handleTextChanged(text: String) {
        if (text.isEmpty()) {
            popupWindow?.dismiss()
            return
        }

        // 获取当前光标前的单词
        val cursorPosition = selectionStart
        val textBeforeCursor = text.take(cursorPosition)
        val lastSpaceIndex = textBeforeCursor.lastIndexOf(',')
        val currentWord = if (lastSpaceIndex == -1) {
            textBeforeCursor.trim()
        } else {
            textBeforeCursor.substring(lastSpaceIndex + 1).trim()
        }

        if (currentWord.isNotEmpty()) {
            showSuggestions(currentWord)
        } else {
            popupWindow?.dismiss()
        }
    }

    private fun showSuggestions(prefix: String) {
        val filtered = suggestions.filter {
            it.startsWith(prefix, ignoreCase = true)
        }.take(10) // 限制显示数量

        if (filtered.isEmpty()) {
            popupWindow?.dismiss()
            return
        }

        val cursorRect = Rect()
        getFocusedRect(cursorRect)

        // 使PopupWindow出现在光标下方
        val x = cursorRect.left
        val y = cursorRect.bottom

        if (popupWindow?.isShowing == true) {
            popupWindow?.update(this, x, y, -1, -1)
        } else {
            popupWindow?.showAsDropDown(this, x, y);
        }
        post {
            suggestionAdapter?.clear()
            suggestionAdapter?.addAll(filtered)
            suggestionAdapter?.notifyDataSetChanged()

        }
    }

    private fun insertSuggestion(suggestion: String) {
        val text = this.text.toString()
        val cursorPosition = selectionStart

        // 找到当前正在输入的单词的起始位置
        val textBeforeCursor = text.take(cursorPosition)
        val lastSpaceIndex = textBeforeCursor.lastIndexOf(',')
        val startReplace = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1

        // 替换当前单词为建议词
        val newText = text.take(startReplace) + suggestion + "," + text.substring(cursorPosition)
        setText(newText)

        // 移动光标到新位置
        setSelection(startReplace + suggestion.length + 1)

        popupWindow?.dismiss()
    }

    fun setCustomSuggestions(customList: List<String>) {
        suggestions.clear()
        suggestions.addAll(customList.sorted())
        suggestionAdapter?.notifyDataSetChanged()
    }

    fun addSuggestions(newSuggestions: List<String>) {
        suggestions.addAll(newSuggestions)
        suggestions = suggestions.distinct().sorted().toMutableList()
        suggestionAdapter?.notifyDataSetChanged()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        popupWindow?.dismiss()
    }
}