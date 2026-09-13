package com.t25hash.nothingwidget

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

/**
 * ウィジェットタップで開く「パパっとメモ」画面。
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * ダイアログ風の透過テーマでこのActivityを一瞬だけ開き、共有はOS標準のシェアシート
 * (ACTION_SEND)に丸投げする。Essential SpaceやAIアプリなど、端末にインストールされていて
 * テキスト共有を受け取れるアプリはすべて宛先候補としてシェアシートに出てくる
 * (このアプリ側で宛先を個別対応する必要はない)。
 */
class QuickMemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_memo)

        val memoInput = findViewById<EditText>(R.id.memo_input)
        val shareButton = findViewById<Button>(R.id.share_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)

        memoInput.requestFocus()

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(sendIntent, null))
            }
            finish()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
}
