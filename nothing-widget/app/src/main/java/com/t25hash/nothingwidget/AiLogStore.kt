package com.t25hash.nothingwidget

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 他のAIアプリ(Perplexity/ChatGPT等)の共有画面から「共有」で送られてきた
 * テキストを、上書きせずMarkdownファイルに追記していくだけのログ。
 * 保存先はこのアプリ専用の内部ストレージ(他アプリ・他ユーザーからは見えない)。
 * 外に出したい時だけAiLogViewActivityの共有ボタンからエクスポートする。
 */
object AiLogStore {
    private const val FILE_NAME = "ai_log.md"

    fun append(context: Context, text: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPAN).format(Date())
        val entry = "## $timestamp\n\n$text\n\n"
        file(context).appendText(entry)
    }

    fun readAll(context: Context): String {
        val f = file(context)
        return if (f.exists()) f.readText() else ""
    }

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}
