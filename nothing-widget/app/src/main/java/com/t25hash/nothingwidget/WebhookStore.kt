package com.t25hash.nothingwidget

import android.content.Context

object WebhookStore {
    private const val PREFS = "quick_memo"
    private const val KEY_WEBHOOK_URL = "discord_webhook_url"

    fun url(context: Context): String = prefs(context).getString(KEY_WEBHOOK_URL, "") ?: ""

    fun setUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_WEBHOOK_URL, url).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
