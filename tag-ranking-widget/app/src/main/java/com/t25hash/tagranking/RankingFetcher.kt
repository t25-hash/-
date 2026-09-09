package com.t25hash.tagranking

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RankedArticle(
    val source: String,
    val title: String,
    val url: String,
    val likes: Int,
    val tag: String,
)

/**
 * Qiitaは公式APIなので問題なし。
 * Zennには公式APIが無く、サイト内部の非公式検索エンドポイント(zenn.dev/api/search)を叩く。
 * 引数・レスポンス形状は文書化されていないため、候補を順に試し、
 * 入れ物のキー名も複数候補から探す(bandersnach/src/search.jsの実装を踏襲)。
 * noteは対象外(規約でスクレイピング禁止)。
 */
object RankingFetcher {

    private const val USER_AGENT = "tag-ranking-widget (personal use)"
    private const val MAX_PER_SOURCE = 10

    private val ZENN_ARTICLE_CONTAINER_KEYS = listOf("articles", "results", "items", "search_results")

    fun fetchQiita(tag: String, sinceDays: Int = 7): List<RankedArticle> {
        return try {
            val since = isoDay(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(sinceDays.toLong()))
            val query = URLEncoder.encode("tag:$tag created:>=$since", "UTF-8")
            val url = URL("https://qiita.com/api/v2/items?query=$query&per_page=50")
            val json = httpGetJsonArray(url) ?: return emptyList()
            (0 until json.length()).mapNotNull { i ->
                val item = json.optJSONObject(i) ?: return@mapNotNull null
                val title = item.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val itemUrl = item.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                RankedArticle(
                    source = "Qiita",
                    title = title,
                    url = itemUrl,
                    likes = item.optInt("likes_count", 0),
                    tag = tag,
                )
            }.sortedByDescending { it.likes }.take(MAX_PER_SOURCE)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun fetchZenn(tag: String): List<RankedArticle> {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val candidates = listOf(
            "https://zenn.dev/api/search?q=$encodedTag",
            "https://zenn.dev/api/search?q=$encodedTag&source=articles",
        )
        for (candidateUrl in candidates) {
            try {
                val json = httpGetJson(URL(candidateUrl)) ?: continue
                val articles = ZENN_ARTICLE_CONTAINER_KEYS
                    .mapNotNull { key -> json.optJSONArray(key) }
                    .firstOrNull() ?: continue
                val result = (0 until articles.length()).mapNotNull { i ->
                    val a = articles.optJSONObject(i) ?: return@mapNotNull null
                    val title = a.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val path = a.optString("path").takeIf { it.isNotBlank() }
                    val articleUrl = when {
                        path != null -> "https://zenn.dev$path"
                        else -> a.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    }
                    RankedArticle(
                        source = "Zenn",
                        title = title,
                        url = articleUrl,
                        likes = a.optInt("liked_count", 0),
                        tag = tag,
                    )
                }
                if (result.isNotEmpty()) {
                    return result.sortedByDescending { it.likes }.take(MAX_PER_SOURCE)
                }
            } catch (e: Exception) {
                // 次の候補へ
            }
        }
        return emptyList()
    }

    private fun httpGetJsonArray(url: URL): JSONArray? {
        val text = httpGet(url) ?: return null
        return try { JSONArray(text) } catch (e: Exception) { null }
    }

    private fun httpGetJson(url: URL): JSONObject? {
        val text = httpGet(url) ?: return null
        return try { JSONObject(text) } catch (e: Exception) { null }
    }

    private fun httpGet(url: URL): String? {
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun isoDay(epochMillis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(epochMillis))
    }
}
