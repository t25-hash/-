package com.t25hash.naroureader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val url: String, val title: String, val savedAt: Long)

/**
 * URL・タイトル・保存日時だけを保存する(本文は一切保存しない)。
 */
object BookmarkStore {
    private const val PREFS = "bookmarks"
    private const val KEY = "list"

    fun all(context: Context): List<Bookmark> {
        val raw = prefs(context).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Bookmark(o.getString("url"), o.getString("title"), o.getLong("savedAt"))
        }.sortedByDescending { it.savedAt }
    }

    fun add(context: Context, url: String, title: String) {
        val current = all(context).filterNot { it.url == url }
        val updated = current + Bookmark(url, title.ifBlank { url }, System.currentTimeMillis())
        val arr = JSONArray()
        updated.forEach {
            arr.put(
                JSONObject().apply {
                    put("url", it.url)
                    put("title", it.title)
                    put("savedAt", it.savedAt)
                },
            )
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
