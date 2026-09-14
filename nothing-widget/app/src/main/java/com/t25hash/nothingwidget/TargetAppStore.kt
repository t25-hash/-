package com.t25hash.nothingwidget

import android.content.ComponentName
import android.content.Context
import org.json.JSONObject

/**
 * 「一括送信」の送信先アプリを覚えておくストア。
 * パッケージ名を決め打ちすると間違える/変わるリスクがあるため、
 * 各スロット(Perplexity等)は初回だけOSの共有シートから選んでもらい、
 * 選ばれたComponentNameを記憶する。2回目以降はそこへ直接送る。
 */
object TargetAppStore {
    private const val PREFS = "quick_memo"
    private const val KEY_TARGETS = "send_targets"

    val SLOT_LABELS = listOf("Perplexity", "Genspark", "DeepSeek", "Grok")

    fun component(context: Context, label: String): ComponentName? {
        val obj = allComponents(context).optJSONObject(label) ?: return null
        val pkg = obj.optString("package").takeIf { it.isNotBlank() } ?: return null
        val cls = obj.optString("class").takeIf { it.isNotBlank() } ?: return null
        return ComponentName(pkg, cls)
    }

    fun setComponent(context: Context, label: String, component: ComponentName) {
        val all = allComponents(context)
        all.put(
            label,
            JSONObject().apply {
                put("package", component.packageName)
                put("class", component.className)
            },
        )
        prefs(context).edit().putString(KEY_TARGETS, all.toString()).apply()
    }

    private fun allComponents(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_TARGETS, "{}") ?: "{}"
        return JSONObject(raw)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
