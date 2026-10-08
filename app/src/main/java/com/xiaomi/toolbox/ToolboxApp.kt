package com.xiaomi.toolbox

import android.app.Application
import android.os.UserManager
import com.xiaomi.toolbox.boot.DailySyncReceiver
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.overlay.FpsOverlayService

class ToolboxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val um = getSystemService(UserManager::class.java)
        if (um != null && !um.isUserUnlocked) return   // direct boot: CE storage not ready
        val store = SettingsStore(this)
        store.seedDefaults()
        DailySyncReceiver.schedule(this)
        try {
            FpsOverlayService.startIfEnabled(this)
        } catch (_: Throwable) {
            // background FGS starts can throw ForegroundServiceStartNotAllowedException
        }
    }
}
