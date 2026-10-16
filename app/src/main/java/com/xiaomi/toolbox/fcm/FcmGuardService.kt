package com.xiaomi.toolbox.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.xiaomi.toolbox.MainActivity
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.FcmGuard
import com.xiaomi.toolbox.data.SettingsStore

/**
 * Watchdog for MILLET_NO_RESTRICT_APP: repairs right after HyperOS rewrites the
 * key (content observer, debounced) plus an in-process 30 min fallback that
 * does not wake a sleeping phone. Kicks FCM only after a real repair.
 */
class FcmGuardService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null
    private var foreground = false
    private val store by lazy { SettingsStore(this) }

    private val fallback = object : Runnable {
        override fun run() {
            repair(notifyFailure = false)
            handler.postDelayed(this, FALLBACK_MS)
        }
    }
    private val debounced = Runnable { repair(notifyFailure = true) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        applyMode()
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                handler.removeCallbacks(debounced)
                handler.postDelayed(debounced, 400L)
            }
        }
        contentResolver.registerContentObserver(Settings.System.getUriFor(FcmGuard.KEY), false, obs)
        observer = obs
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FcmGuard.isAutoEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        applyMode()
        handler.removeCallbacks(fallback)
        handler.post(fallback)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        observer?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Throwable) {
            }
        }
        super.onDestroy()
    }

    private fun repair(notifyFailure: Boolean) {
        val repaired = try {
            FcmGuard.repairAndKick(this, store)
        } catch (_: Throwable) {
            false
        }
        if (!foreground) return
        when {
            repaired -> refresh(getString(R.string.fcm_notif_repaired))
            notifyFailure && !FcmGuard.status(store).present -> refresh(getString(R.string.fcm_notif_failed))
        }
    }

    private fun applyMode() {
        if (FcmGuard.usePersistentNotification(this)) {
            ensureChannel(this)
            startForeground(NOTIF_ID, build(getString(R.string.fcm_notif_active)))
            foreground = true
        } else {
            if (foreground) stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
    }

    private fun refresh(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, build(text))
    }

    private fun build(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(getString(R.string.fcm_guard_title))
            .setContentText(text)
            .setContentIntent(pi)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL = "fcm_guard_channel"
        private const val NOTIF_ID = 426
        private const val FALLBACK_MS = 30L * 60L * 1000L

        /** Visible-but-silent channel for the persistent notification. */
        fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    ctx.getString(R.string.fcm_notif_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setShowBadge(false)
                    enableVibration(false)
                    enableLights(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                },
            )
        }

        /** True when the ongoing notification would actually reach the shade. */
        fun canNotify(ctx: Context): Boolean {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            if (!nm.areNotificationsEnabled()) return false
            val ch = nm.getNotificationChannel(CHANNEL) ?: return true
            return ch.importance != NotificationManager.IMPORTANCE_NONE
        }

        /** Starts (or re-applies mode on) the watchdog when auto-protection is on. */
        fun startIfEnabled(ctx: Context) {
            if (!FcmGuard.isAutoEnabled(ctx)) return
            val i = Intent(ctx, FcmGuardService::class.java)
            try {
                if (FcmGuard.usePersistentNotification(ctx)) ctx.startForegroundService(i) else ctx.startService(i)
            } catch (_: Throwable) {
                // background start can be refused by HyperOS battery policy
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, FcmGuardService::class.java))
        }
    }
}
