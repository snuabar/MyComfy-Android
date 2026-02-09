package com.snuabar.mycomfy.view

// PromptEditText.kt
import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import com.snuabar.mycomfy.R
import com.snuabar.mycomfy.databinding.LayoutSuggestionPopupBinding
import com.snuabar.mycomfy.main.data.prompt.AdvancedTranslator
import com.snuabar.mycomfy.main.data.prompt.PromptManager
import com.snuabar.mycomfy.utils.ViewUtils

class PromptEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var popupWindow: PopupWindow? = null
    private var suggestionAdapter: SuggestionAdapter? = null
    private var suggestions: MutableList<String> = mutableListOf()
    private val popupViewBinding: LayoutSuggestionPopupBinding
    private var directlyInsert: Boolean = false
    var showSuggestions: Boolean = true
        set(value) {
            field = value
            if (!value) {
                popupWindow?.dismiss()
            }
        }
    private var untranslatedText: String? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupViewBinding = LayoutSuggestionPopupBinding.inflate(inflater)

        setupPopupWindow()
        loadDefaultSuggestions()

        // 监听文本变化
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handleTextChanged(s?.toString() ?: "")
            }
        })
    }

    private fun setupPopupWindow() {
        suggestionAdapter = SuggestionAdapter(context)
        popupViewBinding.suggestionList.adapter = suggestionAdapter

        // 设置ListView点击事件
        popupViewBinding.suggestionList.setOnItemClickListener { _, _, position, _ ->
            val selected = suggestionAdapter?.getItem(position) as String
            insertSuggestion(selected)
        }

        popupWindow = PopupWindow(
            popupViewBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            // 设置背景，让PopupWindow可以响应外部点击
            setBackgroundDrawable (AppCompatResources.getDrawable(
                context,
                R.drawable.popup_bg
            ))
            isOutsideTouchable = true
            elevation = 8.0f
        }
    }

    private fun loadDefaultSuggestions() {
        val set = HashSet<String>()
        // 加载提示词常用词汇库
        PromptManager.getInstance().getAllCategories().forEach { (_, _, keywords) ->
            set.addAll(keywords)
        }
        suggestions.addAll(set)
        suggestions.sort()
        AdvancedTranslator.getInstance()?.translateBatch(suggestions, "en", "zh") { }
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

        if (currentWord.isNotEmpty() && showSuggestions) {
            showSuggestions(currentWord)
        } else {
            popupWindow?.dismiss()
        }
    }

    private fun measurePopupWindowSize() {
        // 手动测量和布局
        ViewUtils.measure(popupViewBinding.root)
    }

    private fun showSuggestions(prefix: String) {
        val filtered = suggestions.filter {
            it.startsWith(prefix, ignoreCase = true)
        }.take(10) // 限制显示数量

        if (filtered.isEmpty()) {
            popupWindow?.dismiss()
            return
        }

        directlyInsert = false

        val cursorRect = Rect()
        getFocusedRect(cursorRect)

        // 使PopupWindow出现在光标下方
        val x = cursorRect.right
        val y = cursorRect.bottom - (height + scrollY + paddingBottom)

//        popupWindow?.isOutsideTouchable = false
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

    fun showPrompts(anchor: View, prompts: List<String>) {
        if (prompts.isEmpty()) {
            return
        }

        directlyInsert = true

        measurePopupWindowSize()

        // 使PopupWindow出现在光标下方
        val x = 0
        val y = -anchor.height - popupViewBinding.suggestionList.height

//        popupWindow?.isOutsideTouchable = true
        if (popupWindow?.isShowing == true) {
            popupWindow?.update(anchor, x, y, -1, -1)
        } else {
            popupWindow?.showAsDropDown(anchor, x, y);
        }

        post {
            suggestionAdapter?.clear()
            suggestionAdapter?.addAll(prompts)
            suggestionAdapter?.notifyDataSetChanged()
        }
    }

    fun showPromptsAsDropDown(anchor: View, prompts: List<String>) {
        if (prompts.isEmpty()) {
            return
        }

        directlyInsert = true

        measurePopupWindowSize()

        // 使PopupWindow出现在光标下方
        val x = 0
        val y = 0

//        popupWindow?.isOutsideTouchable = true
        if (popupWindow?.isShowing == true) {
            popupWindow?.update(anchor, x, y, -1, -1)
        } else {
            popupWindow?.showAsDropDown(anchor, x, y);
        }

        post {
            suggestionAdapter?.clear()
            suggestionAdapter?.addAll(prompts)
            suggestionAdapter?.notifyDataSetChanged()
        }
    }

    private fun insertSuggestion(suggestion: String) {
        val text = this.text.toString()
        val cursorPosition = selectionStart

        // 找到当前正在输入的单词的起始位置
        val textBeforeCursor = text.take(cursorPosition)
        if (directlyInsert) {// 替换当前单词为建议词
            val beforeSuggestion = if (textBeforeCursor.isNotEmpty() && textBeforeCursor.last() != ',') "," else ""
            val afterSuggestion = (if (cursorPosition == text.length) "," else (if (text[cursorPosition] != ',') { ","} else {""}))
            val finalSuggestion = beforeSuggestion + suggestion + afterSuggestion
            val newText = textBeforeCursor + finalSuggestion + text.substring(cursorPosition)

            setText(newText)

            // 移动光标到新位置
            setSelection(textBeforeCursor.length + finalSuggestion.length)
        } else {
            val lastSpaceIndex = textBeforeCursor.lastIndexOf(',')
            val startReplace = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1

            // 替换当前单词为建议词
            val newText =
                text.take(startReplace) + suggestion + "," + text.substring(cursorPosition)
            setText(newText)

            // 移动光标到新位置
            setSelection(startReplace + suggestion.length + 1)
        }

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

    fun translatePromptToZH() {
        if (untranslatedText == null) {
            untranslatedText = text.toString()
        }
        val list = untranslatedText?.split(',', '，', '.', '。')
        if (list != null) {
            AdvancedTranslator.getInstance()?.translateBatchToZhAuto(list) {
                if (it != null) {
                    setText(it.joinToString("，").lowercase())
                    setSelection(text?.length ?: 0)
                }
            }
        }
    }

    fun translatePromptToEN() {
        if (untranslatedText == null) {
            untranslatedText = text.toString()
        }
        val list = untranslatedText?.split(',', '，', '.', '。')
        if (list != null) {
            AdvancedTranslator.getInstance()?.translateBatchToEnAuto(list) {
                if (it != null) {
                    setText(it.joinToString(",").lowercase())
                    setSelection(text?.length ?: 0)
                }
            }
        }
    }

    fun translateNone() {
        setText(untranslatedText)
        setSelection(text?.length ?: 0)
        untranslatedText = null;
    }

    private class SuggestionAdapter(context: Context) :
        ArrayAdapter<String>(context, R.layout.layout_suggestion_item, R.id.text1) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = super.getView(position, convertView, parent)
            val text0 = v.findViewById<TextView>(R.id.text0)
            val text1 = v.findViewById<TextView>(R.id.text1)
            text0.text = if (getItem(position) != null) {
                val text = getItem(position) ?: ""
                AdvancedTranslator.getInstance()?.get(text, "en", "zh") ?: (text1.text ?: "")
            } else {
                text1.text ?: ""
            }
            return v
        }
    }
}