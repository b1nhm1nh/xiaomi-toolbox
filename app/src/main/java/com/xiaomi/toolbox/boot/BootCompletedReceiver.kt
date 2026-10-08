package com.xiaomi.toolbox.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.xiaomi.toolbox.data.Catalog
import com.xiaomi.toolbox.data.FcmGuard
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.overlay.FpsOverlayService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val um = app.getSystemService(UserManager::class.java)
        if (um != null && !um.isUserUnlocked) return   // never run before CE storage is ready
        val pending = goAsync()
        Thread {
            try {
                val store = SettingsStore(app)
                store.seedDefaults()
                DailySyncReceiver.schedule(app)
                FpsOverlayService.startIfEnabled(app)
                try {
                    FcmGuard.repairAndKick(app, store)
                } catch (_: Throwable) {
                }
                try {
                    Catalog(app).dailySync(store)
                } catch (_: Throwable) {
                }
            } catch (_: Throwable) {
            } finally {
                pending.finish()
            }
        }.start()
    }
}
