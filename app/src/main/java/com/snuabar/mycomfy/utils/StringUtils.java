package com.snuabar.mycomfy.utils;

public class StringUtils {

    /**
     * 判断字符串是否主要是中文字符
     * @param str 要检查的字符串
     * @return 主要包含中文返回true，否则返回false
     */
    public static boolean isChineseString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        int chineseCount = 0;
        int totalCount = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            totalCount++;

            // 检查字符是否在中文Unicode范围内
            // 基本汉字：0x4E00-0x9FA5
            // 扩展A：0x3400-0x4DBF
            // 扩展B：0x20000-0x2A6DF（需要处理代理对）
            if (isChineseCharacter(c)) {
                chineseCount++;
            } else if (Character.isWhitespace(c) || isPunctuation(c)) {
                // 跳过空格和标点
                totalCount--;
            }
        }

        // 如果中文比例超过70%，认为是中文字符串
        return totalCount > 0 && (chineseCount * 100 / totalCount >= 70);
    }

    /**
     * 判断单个字符是否是中文
     */
    public static boolean isChineseCharacter(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
                || ub == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT;
    }

    /**
     * 判断字符串是否主要是英文字符
     */
    public static boolean isEnglishString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        int englishCount = 0;
        int totalCount = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            totalCount++;

            // 检查是否是英文字母
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                englishCount++;
            } else if (Character.isWhitespace(c) || isPunctuation(c)) {
                // 跳过空格和标点
                totalCount--;
            }
        }

        return totalCount > 0 && (englishCount * 100 / totalCount >= 70);
    }

    /**
     * 综合判断：返回字符串类型
     * @return 0: 未知/混合, 1: 中文, 2: 英文
     */
    public static int getStringLanguageType(String str) {
        if (isChineseString(str)) {
            return 1;
        } else if (isEnglishString(str)) {
            return 2;
        } else {
            return 0;
        }
    }

    private static boolean isPunctuation(char c) {
        return c == ',' || c == '.' || c == '!' || c == '?' || c == ';'
                || c == ':' || c == '"' || c == '\'' || c == '(' || c == ')'
                || c == '[' || c == ']' || c == '{' || c == '}' || c == '-'
                || c == '_' || c == '。' || c == '，' || c == '！' || c == '？'
                || c == '；' || c == '：' || c == '“' || c == '”' || c == '（'
                || c == '）' || c == '【' || c == '】' || c == '《' || c == '》';
    }

    /**
     * 快速判断字符串语言类型
     * @param str 输入字符串
     * @return "zh": 中文, "en": 英文, "mixed": 混合, "unknown": 未知
     */
    public static String detectLanguage(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "unknown";
        }

        int chineseCharCount = 0;
        int englishCharCount = 0;
        int otherCharCount = 0;

        String trimmedStr = str.trim();

        for (int i = 0; i < trimmedStr.length(); i++) {
            char c = trimmedStr.charAt(i);

            // 判断中文（常用Unicode范围）
            if (c >= 0x4E00 && c <= 0x9FA5) {
                chineseCharCount++;
            }
            // 判断英文字母
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                englishCharCount++;
            } else if (!Character.isWhitespace(c)) {
                otherCharCount++;
            }
        }

        int totalChars = chineseCharCount + englishCharCount + otherCharCount;

        if (totalChars == 0) {
            return "unknown";
        }

        double chineseRatio = (double) chineseCharCount / totalChars;
        double englishRatio = (double) englishCharCount / totalChars;

        if (chineseRatio > 0.7) {
            return "zh";
        } else if (englishRatio > 0.7) {
            return "en";
        } else {
            return "mixed";
        }
    }

    /**
     * 判断是否主要是中文
     */
    public static boolean isMainlyChinese(String str) {
        return "zh".equals(detectLanguage(str));
    }

    /**
     * 判断是否主要是英文
     */
    public static boolean isMainlyEnglish(String str) {
        return "en".equals(detectLanguage(str));
    }
}