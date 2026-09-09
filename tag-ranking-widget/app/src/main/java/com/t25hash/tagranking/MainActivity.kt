package com.t25hash.tagranking

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tagsInput = findViewById<EditText>(R.id.tags_input)
        val saveButton = findViewById<Button>(R.id.save_button)
        val refreshButton = findViewById<Button>(R.id.refresh_button)
        val statusText = findViewById<TextView>(R.id.status_text)

        tagsInput.setText(RankingStore.tags(this).joinToString(", "))
        updateStatus(statusText)

        saveButton.setOnClickListener {
            val tags = tagsInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            RankingStore.setTags(this, tags)
            RankingWorker.runOnce(this)
            Toast.makeText(this, "保存しました。更新中…", Toast.LENGTH_SHORT).show()
        }

        refreshButton.setOnClickListener {
            RankingWorker.runOnce(this)
            Toast.makeText(this, "更新をリクエストしました", Toast.LENGTH_SHORT).show()
        }

        RankingWorker.schedule(this)
    }

    private fun updateStatus(statusText: TextView) {
        val updatedAt = RankingStore.updatedAt(this)
        statusText.text = if (updatedAt == 0L) {
            "まだ取得していません"
        } else {
            "最終更新: " + DateFormat.format("yyyy/MM/dd HH:mm", updatedAt)
        }
    }
}
