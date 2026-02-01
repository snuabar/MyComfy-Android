package com.snuabar.mycomfy.main.data.prompt

import android.content.Context
import com.google.gson.Gson

// 1. 创建数据类
data class PromptCategory(
    val name: String,
    val displayName: String,
    val keywords: List<String>
)

data class PromptDictionary(
    val categories: List<PromptCategory>
)

class PromptManager private constructor(context: Context) {
    private val promptDictionary: PromptDictionary

    init {
        // 读取JSON文件（文件放在assets文件夹中）
        val jsonString = context.assets.open("prompt_dictionary.json")
            .bufferedReader().use { it.readText() }
        promptDictionary = Gson().fromJson(jsonString, PromptDictionary::class.java)
    }

    companion object {
        @Volatile
        private var INSTANCE: PromptManager? = null
        /**
         * 初始化单例实例
         */
        fun init(context: Context) {
            synchronized(this) {
                INSTANCE = PromptManager(context.applicationContext)
            }
        }

        /**
         * 获取已初始化的实例（避免重复传递 context）
         */
        fun getInstance(): PromptManager {
            return INSTANCE ?: throw IllegalStateException(
                "PromptManager must be initialized first. Call getInstance(context) first."
            )
        }
    }

    // 3. 获取所有类别
    fun getAllCategories(): List<PromptCategory> = promptDictionary.categories

    // 4. 按类别获取关键词
    fun getKeywordsByCategory(categoryName: String): List<String> {
        return promptDictionary.categories
            .find { it.name == categoryName }
            ?.keywords ?: emptyList()
    }

    // 5. 搜索关键词（所有类别）
    fun searchKeywords(query: String): List<String> {
        return promptDictionary.categories
            .flatMap { it.keywords }
            .filter { it.contains(query, ignoreCase = true) }
            .distinct()
            .sorted()
    }
}