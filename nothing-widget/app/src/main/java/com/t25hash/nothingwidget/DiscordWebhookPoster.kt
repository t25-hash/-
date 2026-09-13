package com.t25hash.nothingwidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discord公式のWebhook機能(https://discord.com/developers/docs/resources/webhook)。
 * ボット不要・認証不要で、発行されたURLにJSONをPOSTするだけでチャンネルに投稿できる。
 * セルフボットのような規約違反の要素は一切ない、公式にサポートされた仅組み。
 */
object DiscordWebhookPoster {
    fun post(webhookUrl: String, content: String): Boolean {
        val conn = URL(webhookUrl).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = JSONObject().apply { put("content", content) }.toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }
}
