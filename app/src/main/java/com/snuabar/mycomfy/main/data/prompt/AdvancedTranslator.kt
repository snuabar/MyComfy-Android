package com.snuabar.mycomfy.main.data.prompt

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONObject
import java.io.File

class AdvancedTranslator(context: Context) {

    private val translatedMapFile: File;
    private var batchTranslating = false
    private val translatedMap: JSONObject
    private var translatedMapChanged = false

    init {
        val file: File? = context.getExternalFilesDir(null)
        if (file != null && !file.exists() && !file.mkdirs()) {
            Log.e(TAG, "getTranslateMapFile: failed to execute mkdirs()")
        }
        translatedMapFile = File(file, TRANSLATED_MAP_FILE_NAME)
        translatedMap = if (translatedMapFile.exists()) {
            val jsonStr = translatedMapFile.readText()
            val jsonObject = if (!TextUtils.isEmpty(jsonStr)) {
                JSONObject(jsonStr)
            } else {
                JSONObject()
            }
            jsonObject
        } else {
            JSONObject()
        }
    }

    companion object {
        private const val TRANSLATED_MAP_FILE_NAME = "prompt.manager.translated.json"
        private val TAG = Companion::class.java.name
        private var INSTANCE: AdvancedTranslator? = null

        fun init(context: Context): AdvancedTranslator? {
            if (INSTANCE == null) {
                INSTANCE = AdvancedTranslator(context)
            }
            return INSTANCE
        }

        fun getInstance(): AdvancedTranslator? {
            if (INSTANCE == null) {
                throw NullPointerException("use ${Companion::class.qualifiedName}.init(context) to initialize.")
            }
            return INSTANCE
        }
    }

    private val translators = HashMap<String, Translator?>()
//    private var currentTranslatorKey: String? = "en-zh"

    // 翻译状态回调接口
    interface TranslationCallback {
        fun onResult(translatedText: String?, error: String?)
    }

    interface DownloadStateCallback {
        fun onDownloadProgress(progress: Int, msg: String?) // 下载进度
    }

    /**
     * 初始化翻译器
     */
    fun initTranslator(
        sourceLang: String,
        targetLang: String,
        downloadIfNeeded: Boolean = false,
        callback: DownloadStateCallback? = null
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)
        translators["$sourceLang-$targetLang"] = translator

        if (downloadIfNeeded) {
            downloadModelIfNeeded(translator, callback)
        }
    }

    /**
     * 下载语言模型（如果需要）
     */
    private fun downloadModelIfNeeded(
        translator: Translator,
        callback: DownloadStateCallback?
    ) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()  // 可根据需求调整条件
            .build()

        // 下载源语言模型
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d("Download", "源语言模型已准备好")
                // 下载目标语言模型
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.d("Download", "目标语言模型已准备好")
                        callback?.onDownloadProgress(100, null)
                    }
                    .addOnFailureListener { e ->
                        callback?.onDownloadProgress(0, "目标语言模型下载失败: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                callback?.onDownloadProgress(0, "源语言模型下载失败: ${e.message}")
            }
    }

    fun get(text: String, srcLang: String, destLang: String): String? =
        if (translatedMap.has("$srcLang-$destLang")) {
            val map = translatedMap.optJSONObject("$srcLang-$destLang")
            if (map?.has(text) == true) {
                map.optString(text, text)
            } else {
                null
            }
        } else {
            null
        }

    /**
     * 执行翻译
     */
    fun translate(text: String, srcLang: String, destLang: String, callback: TranslationCallback) {
        if (TextUtils.isEmpty(text)) {
            callback.onResult(text, null)
            return
        }
        if (get(text, srcLang, destLang) != null) {
            callback.onResult(get(text, srcLang, destLang), null)
            return
        }

        val translatorKey = "$srcLang-$destLang"
        val currentTranslator = translators[translatorKey]
        currentTranslator?.let { translator ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    var map = translatedMap.optJSONObject(translatorKey)
                    if (map == null) {
                        map = JSONObject()
                    }
                    map.putOpt(text, translatedText)
                    translatedMap.putOpt(translatorKey, map)
                    translatedMapChanged = true
                    if (!batchTranslating) {
                        translatedMapFile.writeText(translatedMap.toString())
                    }
                    callback.onResult(translatedText, null)
                }
                .addOnFailureListener { exception ->
                    callback.onResult(null, "翻译失败: ${exception.message}")
                }
        } ?: run {
            callback.onResult(null, "翻译器未初始化")
        }
    }

    /**
     * 批量翻译
     */
    fun translateBatch(
        texts: List<String>, srcLang: String, destLang: String, callback: (List<String?>?) -> Unit) {
        val translatedResults = mutableListOf<String?>()
        var completedCount = 0

        batchTranslating = true

        texts.forEach { text ->
            translate(text, srcLang, destLang, object : TranslationCallback {
                override fun onResult(translatedText: String?, error: String?) {
                    if (error != null) {
                        Log.e("BatchTranslation", error)
                        completedCount++

                        if (completedCount == texts.size) {
                            callback(translatedResults.ifEmpty { null })
                        }
                        return
                    }
                    translatedResults.add(translatedText)
                    completedCount++

                    if (completedCount == texts.size) {
                        if (translatedMapChanged) {
                            translatedMapFile.writeText(translatedMap.toString())
                            translatedMapChanged = false
                        }
                        batchTranslating = false
                        callback(translatedResults)
                    }
                }
            })
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        translators.forEach { (_, value) ->
            run {
                value?.close()
            }
        }
        translators.clear()
    }

    /**
     * 检查语言是否支持
     */
    fun getSupportedLanguages(): List<String> {
        return TranslateLanguage.getAllLanguages().map { it }
    }

    /**
     * 获取语言显示名称
     */
    fun getLanguageName(languageCode: String): String {
        return try {
            TranslateLanguage.fromLanguageTag(languageCode) ?: languageCode
        } catch (e: Exception) {
            languageCode
        }
    }
}