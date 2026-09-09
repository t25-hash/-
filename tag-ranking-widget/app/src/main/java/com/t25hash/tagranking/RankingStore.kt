package com.t25hash.tagranking

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RankingStore {
    private const val PREFS = "tag_ranking"
    private const val KEY_TAGS = "tags"
    private const val KEY_CACHE = "cache"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val DEFAULT_TAG = "Android"

    fun tags(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TAGS, null)
        if (raw.isNullOrBlank()) return listOf(DEFAULT_TAG)
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setTags(context: Context, tags: List<String>) {
        prefs(context).edit().putString(KEY_TAGS, tags.joinToString(",")).apply()
    }

    fun cache(context: Context): List<RankedArticle> {
        val raw = prefs(context).getString(KEY_CACHE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RankedArticle(
                source = o.getString("source"),
                title = o.getString("title"),
                url = o.getString("url"),
                likes = o.getInt("likes"),
                tag = o.getString("tag"),
            )
        }
    }

    fun saveCache(context: Context, articles: List<RankedArticle>) {
        val arr = JSONArray()
        articles.forEach {
            arr.put(
                JSONObject().apply {
                    put("source", it.source)
                    put("title", it.title)
                    put("url", it.url)
                    put("likes", it.likes)
                    put("tag", it.tag)
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
