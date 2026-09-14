package com.t25hash.nothingwidget

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

/** 蓄積したAIログ(ai_log.md)を表示し、必要な時だけ外部アプリへ共有できる画面。 */
class AiLogViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_log)

        val logText = findViewById<TextView>(R.id.log_text)
        val shareButton = findViewById<ImageButton>(R.id.share_log_button)

        logText.text = AiLogStore.readAll(this).ifBlank { "まだ何も追記されていません" }

        shareButton.setOnClickListener {
            val file = AiLogStore.file(this)
            if (!file.exists()) return@setOnClickListener
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, null))
        }
    }
}
