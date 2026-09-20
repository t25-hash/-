package com.t25hash.rotationcontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 透明な1x1のオーバーレイウィンドウ(TYPE_APPLICATION_OVERLAY)にscreenOrientationを設定し、
 * WindowManagerに追加するだけで、フォアグラウンドアプリの実効的な画面向きに割り込める。
 * (WindowContainer#getOrientation()はZ順で上にある子から先に確認するため、
 * 一番上に乗るオーバーレイの指定が勝つ。AOSPソース(WindowContainer.java)で確認済み)
 */
class ForceOrientationService : Service() {

    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        addOverlay()
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    private fun addOverlay() {
        if (overlayView != null) return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = View(this)
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        windowManager.addView(view, params)
        overlayView = view
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
        overlayView = null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "縦固定", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ForceOrientationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("画面を縦に固定中")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "解除", stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "force_orientation"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.t25hash.rotationcontrol.STOP"

        var isRunning: Boolean = false
            private set
    }
}
