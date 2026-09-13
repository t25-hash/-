package com.t25hash.nothingwidget

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * ウィジェットタップで開く「パパっとメモ」画面。メモ帳アプリのように、
 * テキスト入力欄が画面のほぼ全部を占め、右上に小さいアイコンボタン
 * (Discordに投稿・共有)だけを置くミニマルな見た目にしている。
 * 閉じる操作は専用ボタンを置かず、システムの戻る操作に任せる。
 *
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * 透過テーマでこのActivityを一瞬だけ開く。共有はOS標準のシェアシート(ACTION_SEND)に
 * 丸投げする。Essential SpaceやAIアプリなど、端末にインストールされていてテキスト共有を
 * 受け取れるアプリはすべて宛先候補としてシェアシートに出てくる(このアプリ側で宛先を
 * 個別対応する必要はない)。
 *
 * Discordだけは公式のWebhook機能(ボット不要・認証不要でURLにPOSTするだけの仕組み)を
 * 使い、共有シートを経由せず直接投稿する。Webhook URLは初回投稿時か、Discordボタンの
 * 長押しでダイアログから設定する。
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
        val discordButton = findViewById<ImageButton>(R.id.discord_button)

        memoInput.requestFocus()

        var lastSavedText: String? = null
        fun saveMemoIfChanged(text: String) {
            if (text.isNotEmpty() && text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
            }
        }

        fun promptForWebhookUrl(onSaved: (String) -> Unit) {
            val input = EditText(this).apply {
                setText(WebhookStore.url(this@QuickMemoActivity))
                hint = getString(R.string.webhook_hint)
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.webhook_dialog_title)
                .setView(input)
                .setPositiveButton(R.string.dialog_save) { _, _ ->
                    val url = input.text.toString().trim()
                    if (url.isNotEmpty()) {
                        WebhookStore.setUrl(this, url)
                        onSaved(url)
                    }
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        fun postToDiscord(url: String, text: String) {
            saveMemoIfChanged(text)
            Thread {
                val ok = DiscordWebhookPoster.post(url, text)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (ok) "Discordに投稿しました" else "投稿に失敗しました",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }.start()
        }

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            saveMemoIfChanged(text)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, null))
            // ここでfinish()しない: 同じメモをEssential Space・AIアプリなど
            // 複数の宛先へ続けて共有できるようにするため。閉じるのは戻る操作で。
        }

        discordButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "メモが空です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val existingUrl = WebhookStore.url(this)
            if (existingUrl.isEmpty()) {
                promptForWebhookUrl { url -> postToDiscord(url, text) }
            } else {
                postToDiscord(existingUrl, text)
            }
        }

        discordButton.setOnLongClickListener {
            promptForWebhookUrl { }
            true
        }
    }
}
