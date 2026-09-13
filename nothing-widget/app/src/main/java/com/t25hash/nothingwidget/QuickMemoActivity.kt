package com.t25hash.nothingwidget

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
 *
 * フォントはアプリ・ウィジェット共通でMonospace(等幅)に統一している。
 * ウィジェット側は別プロセス描画(RemoteViews/Glance)のためカスタムフォントを
 * 読み込めず、Glance標準のFontFamily.Monospaceしか使えない制約があるため、
 * こちらの画面もそれに合わせている。
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

        memoInput.typeface = Typeface.MONOSPACE

        // 直近に保存したメモを下書きとして復元する(ウィジェット本体にも同じ内容が出ている)。
        var lastSavedText: String? = MemoStore.lastText(this)
        memoInput.setText(lastSavedText ?: "")
        memoInput.setSelection(memoInput.text.length)
        memoInput.requestFocus()

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
                // Glance公式の更新API。ウィジェットの再描画は別プロセスなので、
                // この呼び出しなしでは保存内容が画面上に反映されない。
                CoroutineScope(Dispatchers.Main).launch {
                    NothingWidget().updateAll(this@QuickMemoActivity)
                }
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
