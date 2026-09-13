package com.t25hash.discordwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "open_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlToOpen = intent.getStringExtra(EXTRA_URL)
        if (urlToOpen != null) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val tokenInput = findViewById<EditText>(R.id.token_input)
        val channelInput = findViewById<EditText>(R.id.channel_input)
        val guildInput = findViewById<EditText>(R.id.guild_input)
        val saveButton = findViewById<Button>(R.id.save_button)
        val refreshButton = findViewById<Button>(R.id.refresh_button)
        val statusText = findViewById<TextView>(R.id.status_text)

        tokenInput.setText(DiscordStore.botToken(this))
        channelInput.setText(DiscordStore.channelId(this))
        guildInput.setText(DiscordStore.guildId(this))
        updateStatus(statusText)

        saveButton.setOnClickListener {
            DiscordStore.setBotToken(this, tokenInput.text.toString().trim())
            DiscordStore.setChannelId(this, channelInput.text.toString().trim())
            DiscordStore.setGuildId(this, guildInput.text.toString().trim())
            Toast.makeText(this, "保存しました。更新中…", Toast.LENGTH_SHORT).show()
            triggerRefreshAndObserve(statusText)
        }

        refreshButton.setOnClickListener {
            Toast.makeText(this, "更新をリクエストしました", Toast.LENGTH_SHORT).show()
            triggerRefreshAndObserve(statusText)
        }

        DiscordWorker.schedule(this)
    }

    private fun triggerRefreshAndObserve(statusText: TextView) {
        val request = DiscordWorker.runOnce(this)
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                updateStatus(statusText)
            }
        }
    }

    private fun updateStatus(statusText: TextView) {
        val updatedAt = DiscordStore.updatedAt(this)
        val count = DiscordStore.cache(this).size
        statusText.text = if (updatedAt == 0L) {
            "まだ取得していません"
        } else {
            "最終更新: " + DateFormat.format("yyyy/MM/dd HH:mm", updatedAt) + "\n件数: $count"
        }
    }
}
