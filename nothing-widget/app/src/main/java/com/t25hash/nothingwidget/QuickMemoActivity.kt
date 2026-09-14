package com.t25hash.nothingwidget

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ウィジェットタップで開く「パパっとメモ」画面。メモ帳アプリのように、
 * テキスト入力欄が画面のほぼ全部を占め、右上にアイコンだけを置く
 * ミニマルな見た目にしている。閉じる操作は専用ボタンを置かず、
 * システムの戻る操作に任せる。
 *
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * 透過テーマでこのActivityを一瞬だけ開く。共有はOS標準のシェアシート(ACTION_SEND)に
 * 丸投げする。Essential SpaceやAIアプリなど、端末にインストールされていてテキスト共有を
 * 受け取れるアプリはすべて宛先候補としてシェアシートに出てくる(このアプリ側で宛先を
 * 個別対応する必要はない)。
 *
 * 「一括送信」は、チェックしたAIアプリ(Perplexity/Genspark/DeepSeek/Grok)へ
 * 同じ文章を順番にstartActivityする。各アプリの共有/入力画面自体は毎回
 * ユーザーがそのアプリ内で送信を押す必要がある(Androidの仕様上、他アプリへの
 * 送信を裏側で自動完了させることはできない)。送信先アプリはパッケージ名を
 * 決め打ちせず、初回だけ共有シートから選んでもらいTargetAppStoreに記憶する。
 *
 * フォントはアプリ・ウィジェット共通でMonospace(等幅)に統一している。
 */
class QuickMemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_memo)

        // 画面全体を覆う暗いオーバーレイなしで、上部にだけ黒帯が出るようにする。
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.TOP)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val memoInput = findViewById<EditText>(R.id.memo_input)
        val shareButton = findViewById<ImageButton>(R.id.share_button)
        val bulkSendButton = findViewById<ImageButton>(R.id.bulk_send_button)

        memoInput.typeface = Typeface.MONOSPACE

        // 直近に保存したメモを下書きとして復元する(ウィジェット本体にも同じ内容が出ている)。
        var lastSavedText: String? = MemoStore.lastText(this)
        memoInput.setText(lastSavedText ?: "")
        memoInput.setSelection(memoInput.text.length)
        memoInput.requestFocus()

        fun saveIfChanged(text: String) {
            if (text.isNotEmpty() && text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
                // Glance公式の更新API。ウィジェットの再描画は別プロセスなので、
                // この呼び出しなしでは保存内容が画面上に反映されない。
                CoroutineScope(Dispatchers.Main).launch {
                    NothingWidget().updateAll(this@QuickMemoActivity)
                }
            }
        }

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            saveIfChanged(text)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, null))
            // ここでfinish()しない: 同じメモを複数の宛先へ続けて共有できるように
            // するため。閉じるのは戻る操作で。
        }

        bulkSendButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "メモが空です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveIfChanged(text)

            val labels = TargetAppStore.SLOT_LABELS.toTypedArray()
            val checked = BooleanArray(labels.size)
            AlertDialog.Builder(this)
                .setTitle("一括送信するアプリを選択")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setPositiveButton("送信") { _, _ ->
                    labels.forEachIndexed { index, label ->
                        if (checked[index]) sendToSlot(label, text)
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    /** 指定スロットのアプリへ送る。未登録なら共有シートを開いて選んでもらい、以後はそこへ記憶する。 */
    private fun sendToSlot(label: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val registered = TargetAppStore.component(this, label)
        if (registered != null) {
            try {
                startActivity(Intent(sendIntent).setComponent(registered))
                return
            } catch (e: ActivityNotFoundException) {
                // 登録済みアプリが見つからない(アンインストール等) → 選び直してもらう
            }
        }

        val receiverIntent = Intent(this, ChosenComponentReceiver::class.java).apply {
            putExtra(ChosenComponentReceiver.EXTRA_SLOT_LABEL, label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            label.hashCode(),
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val chooser = Intent.createChooser(sendIntent, "${label}として使うアプリを選択", pendingIntent.intentSender)
        startActivity(chooser)
    }
}
