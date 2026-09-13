package com.t25hash.nothingwidget

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

/**
 * ウィジェットタップで開く「パパっとメモ」画面。メモ帳アプリのように、
 * テキスト入力欄が画面のほぼ全部を占め、右上に共有アイコンだけを置く
 * ミニマルな見た目にしている。閉じる操作は専用ボタンを置かず、
 * システムの戻る操作に任せる。
 *
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * 透過テーマでこのActivityを一瞬だけ開く。共有はOS標準のシェアシート(ACTION_SEND)に
 * 丸投げする。Essential SpaceやAIアプリなど、端末にインストールされていてテキスト共有を
 * 受け取れるアプリはすべて宛先候補としてシェアシートに出てくる(このアプリ側で宛先を
 * 個別対応する必要はない)。
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

        memoInput.requestFocus()

        var lastSavedText: String? = null

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, null))
            // ここでfinish()しない: 同じメモを複数の宛先へ続けて共有できるように
            // するため。閉じるのは戻る操作で。
        }
    }
}
