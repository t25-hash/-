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
 * AiLogViewActivityの共有ボタンでエクスポートすると、その直後に本体はクリアされる。
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

    fun clear(context: Context) {
        file(context).writeText("")
    }

    /** 共有用に現在の内容を別ファイルへコピーして返す(共有直後に本体をクリアしても送信済みコピーには影響しない)。 */
    fun exportCopy(context: Context): File {
        val exportDir = File(context.filesDir, "export").apply { mkdirs() }
        val exportFile = File(exportDir, FILE_NAME)
        exportFile.writeText(readAll(context))
        return exportFile
    }

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}
