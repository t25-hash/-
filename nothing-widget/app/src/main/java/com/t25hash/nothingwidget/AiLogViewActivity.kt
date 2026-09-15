package com.t25hash.nothingwidget

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

/** 蓄積したAIログ(ai_log.md)を表示し、必要な時だけ外部アプリへ共有できる画面。共有すると本体はクリアされる。 */
class AiLogViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_log)

        val logText = findViewById<TextView>(R.id.log_text)
        val shareButton = findViewById<ImageButton>(R.id.share_log_button)

        fun refresh() {
            logText.text = AiLogStore.readAll(this).ifBlank { "まだ何も追記されていません" }
        }
        refresh()

        shareButton.setOnClickListener {
            if (AiLogStore.readAll(this).isBlank()) return@setOnClickListener

            val exportFile = AiLogStore.exportCopy(this)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", exportFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, null))

            AiLogStore.clear(this)
            refresh()
        }
    }
}
