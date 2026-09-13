package com.t25hash.discordwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Botトークンを含めてSharedPreferencesに保存する(アプリ専用の私有領域で、
 * root化されていない他アプリからは読めない)。ただしAndroidManifestで
 * allowBackup=falseにして、adb backup/クラウドバックアップ経由での
 * トークン流出を防いでいる。トークン自体は他人に絶対見せないこと。
 */
object DiscordStore {
    private const val PREFS = "discord_widget"
    private const val KEY_TOKEN = "bot_token"
    private const val KEY_CHANNEL_ID = "channel_id"
    private const val KEY_GUILD_ID = "guild_id"
    private const val KEY_CACHE = "cache"
    private const val KEY_UPDATED_AT = "updated_at"

    fun botToken(context: Context): String = prefs(context).getString(KEY_TOKEN, "") ?: ""
    fun setBotToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun channelId(context: Context): String = prefs(context).getString(KEY_CHANNEL_ID, "") ?: ""
    fun setChannelId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_CHANNEL_ID, id).apply()
    }

    fun guildId(context: Context): String = prefs(context).getString(KEY_GUILD_ID, "") ?: ""
    fun setGuildId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_GUILD_ID, id).apply()
    }

    fun cache(context: Context): List<DiscordMessage> {
        val raw = prefs(context).getString(KEY_CACHE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DiscordMessage(
                id = o.getString("id"),
                author = o.getString("author"),
                content = o.getString("content"),
                timestamp = o.getString("timestamp"),
            )
        }
    }

    fun saveCache(context: Context, messages: List<DiscordMessage>) {
        val arr = JSONArray()
        messages.forEach {
            arr.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("author", it.author)
                    put("content", it.content)
                    put("timestamp", it.timestamp)
                },
            )
        }
        prefs(context).edit()
            .putString(KEY_CACHE, arr.toString())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun updatedAt(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
