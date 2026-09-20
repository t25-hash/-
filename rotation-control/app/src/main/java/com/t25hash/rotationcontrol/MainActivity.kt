package com.t25hash.rotationcontrol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 「他のアプリの上に表示」権限(SYSTEM_ALERT_WINDOW)を使い、1x1の透明なオーバーレイウィンドウに
 * screenOrientationを設定して画面に乗せることで、対象アプリがandroid:screenOrientationで
 * 横固定していても縦表示を要求する(公開APIのみ、root/隠しAPI不要)。
 * 参考: https://github.com/curmudgeon-works/curmudgeon-rotation (GPLv3、コードは流用せず手法のみ参照)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var toggle: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggle = findViewById(R.id.force_portrait_switch)
        toggle.isChecked = ForceOrientationService.isRunning

        toggle.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    toggle.isChecked = false
                    Toast.makeText(this, "「他のアプリの上に表示」を許可してください", Toast.LENGTH_LONG).show()
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                    return@setOnCheckedChangeListener
                }
                ContextCompat.startForegroundService(this, Intent(this, ForceOrientationService::class.java))
            } else {
                stopService(Intent(this, ForceOrientationService::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        toggle.isChecked = ForceOrientationService.isRunning
    }
}
