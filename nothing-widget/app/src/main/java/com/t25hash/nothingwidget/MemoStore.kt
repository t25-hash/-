package com.t25hash.nothingwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 書いたメモを端末内に保存しておくだけの単純なストア。
 * 「共有」は複数の宛先に対して繰り返す運用(1つ書いて、Essential Spaceにも
 * AIアプリにも投げる)なので、共有してもActivityは閉じない。保存はテキストが
 * 変わった時だけ追記し、同じメモを何度も共有しても重複保存しない。
 */
object MemoStore {
    private const val PREFS = "quick_memo"
    private const val KEY_MEMOS = "memos"

    fun addMemo(context: Context, text: String) {
        val arr = memosArray(context)
        arr.put(
            JSONObject().apply {
                put("text", text)
                put("created_at", System.currentTimeMillis())
            },
        )
        prefs(context).edit().putString(KEY_MEMOS, arr.toString()).apply()
    }

    private fun memosArray(context: Context): JSONArray {
        val raw = prefs(context).getString(KEY_MEMOS, "[]") ?: "[]"
        return JSONArray(raw)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
