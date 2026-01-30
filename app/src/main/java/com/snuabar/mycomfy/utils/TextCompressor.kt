package com.snuabar.mycomfy.utils

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

object TextCompressor {
    // 压缩
    fun compress(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(text.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    // 解压
    fun decompress(compressed: ByteArray): String {
        GZIPInputStream(compressed.inputStream()).use { gzip ->
            return gzip.readBytes().toString(Charsets.UTF_8)
        }
    }
}