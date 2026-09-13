package com.t25hash.discordwidget

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class DiscordMessage(
    val id: String,
    val author: String,
    val content: String,
    val timestamp: String,
)

/**
 * Discord公式のBot REST APIのみを使う(ユーザートークンによるセルフボットは規約違反のため扱わない)。
 * Botをサーバーに招待し、そのチャンネルを見る権限を与えた上で使う想定。
 * Developer Portalで"Message Content Intent"を有効化しておかないと、
 * 本文が空文字で返ってくることがある。
 */
object DiscordFetcher {
    private const val USER_AGENT = "discord-channel-widget (personal use)"

    fun fetchRecentMessages(botToken: String, channelId: String, limit: Int = 20): List<DiscordMessage> {
        if (botToken.isBlank() || channelId.isBlank()) return emptyList()
        val conn = URL("https://discord.com/api/v10/channels/$channelId/messages?limit=$limit")
            .openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bot $botToken")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            (0 until arr.length()).mapNotNull { i ->
                val m = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = m.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val author = m.optJSONObject("author")?.optString("username") ?: "unknown"
                DiscordMessage(
                    id = id,
                    author = author,
                    content = m.optString("content"),
                    timestamp = m.optString("timestamp"),
                )
            }.reversed() // Discordは新しい順で返すため、古い→新しいに並べ替える
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }
}
