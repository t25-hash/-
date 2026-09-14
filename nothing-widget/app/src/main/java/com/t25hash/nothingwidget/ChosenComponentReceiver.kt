package com.t25hash.nothingwidget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 共有シート(Intent.createChooser)でユーザーが選んだアプリを受け取り、
 * TargetAppStoreにスロット名(Perplexity等)と紐付けて記憶する。
 * これにより2回目以降の「一括送信」はシートを経由せず直接そのアプリへ送れる。
 */
class ChosenComponentReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_SLOT_LABEL = "slot_label"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_SLOT_LABEL) ?: return
        val chosen = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT) ?: return
        TargetAppStore.setComponent(context, label, chosen)
        Toast.makeText(context, "${label}を登録しました", Toast.LENGTH_SHORT).show()
    }
}
