package com.xiaomi.toolbox.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.xiaomi.toolbox.MainActivity
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.Keys

class FpsOverlayService : Service() {
    private var wm: WindowManager? = null
    private var bubble: TextView? = null
    private var frames = 0
    private var lastNs = 0L
    private val choreographer = android.view.Choreographer.getInstance()
    private val callback = object : android.view.Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frames++
            if (lastNs == 0L) lastNs = frameTimeNanos
            val dt = (frameTimeNanos - lastNs) / 1_000_000L
            if (dt >= 500) {
                val fps = frames * 1000f / dt
                bubble?.text = String.format("%.0f", fps)
                frames = 0
                lastNs = frameTimeNanos
            }
            choreographer.postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notif())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this).apply {
            text = "—"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xAA000000.toInt())
            setPadding(16, 8, 16, 8)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }
        var downX = 0
        var downY = 0
        var startX = 0
        var startY = 0
        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX.toInt()
                    downY = e.rawY.toInt()
                    startX = lp.x
                    startY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX.toInt() - downX)
                    lp.y = startY + (e.rawY.toInt() - downY)
                    wm?.updateViewLayout(tv, lp)
                    true
                }
                else -> false
            }
        }
        wm?.addView(tv, lp)
        bubble = tv
        choreographer.postFrameCallback(callback)
    }

    override fun onDestroy() {
        choreographer.removeFrameCallback(callback)
        bubble?.let { v ->
            try {
                wm?.removeView(v)
            } catch (_: Throwable) {
            }
        }
        bubble = null
        super.onDestroy()
    }

    private fun notif(): Notification {
        val ch = "fps_overlay_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(ch, getString(R.string.fps_overlay_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FpsOverlayService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, ch)
            .setContentTitle(getString(R.string.fps_overlay_notif_title))
            .setContentText(getString(R.string.fps_overlay_notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pi)
            .addAction(Notification.Action.Builder(null, getString(R.string.fps_overlay_stop), stop).build())
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            getSharedPreferences(Keys.PREF_FPS, MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    companion object {
        fun startIfEnabled(ctx: android.content.Context) {
            val on = ctx.getSharedPreferences(Keys.PREF_FPS, MODE_PRIVATE).getBoolean("enabled", false)
            if (on && Settings.canDrawOverlays(ctx)) {
                ctx.startForegroundService(Intent(ctx, FpsOverlayService::class.java))
            }
        }
    }
}
