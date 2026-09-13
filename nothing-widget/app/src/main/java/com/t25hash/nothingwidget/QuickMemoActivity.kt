package com.t25hash.nothingwidget

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * ウィジェットタップで開く「パパっとメモ」画面。
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * 透過テーマでこのActivityを一瞬だけ開き、共有はOS標準のシェアシート(ACTION_SEND)に
 * 丸投げする。Essential SpaceやAIアプリなど、端末にインストールされていてテキスト共有を
 * 受け取れるアプリはすべて宛先候補としてシェアシートに出てくる(このアプリ側で宛先を
 * 個別対応する必要はない)。
 *
 * それとは別に、Discordだけは公式のWebhook機能(ボット不要・認証不要でURLにPOSTする
 * だけの仕組み)を使い、共有シートを経由せず直接投稿するボタンも用意している。
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
        val webhookInput = findViewById<EditText>(R.id.webhook_input)
        val shareButton = findViewById<Button>(R.id.share_button)
        val discordPostButton = findViewById<Button>(R.id.discord_post_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)

        webhookInput.setText(WebhookStore.url(this))
        memoInput.requestFocus()

        var lastSavedText: String? = null

        fun saveMemoIfChanged(text: String) {
            if (text.isNotEmpty() && text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
            }
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
            // 複数の宛先へ続けて共有できるようにするため。閉じるのはユーザー操作で。
        }

        discordPostButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            val webhookUrl = webhookInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "メモが空です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (webhookUrl.isEmpty()) {
                Toast.makeText(this, "Webhook URLを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            WebhookStore.setUrl(this, webhookUrl)
            saveMemoIfChanged(text)

            Thread {
                val ok = DiscordWebhookPoster.post(webhookUrl, text)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (ok) "Discordに投稿しました" else "投稿に失敗しました",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }.start()
            // ここでもfinish()しない: 投稿後にさらに共有したい場合があるため。
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
}
