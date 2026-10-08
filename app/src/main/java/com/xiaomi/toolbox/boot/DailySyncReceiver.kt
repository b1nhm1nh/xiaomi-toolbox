package com.xiaomi.toolbox.boot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xiaomi.toolbox.data.Catalog
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.data.SettingsStore
import java.util.Calendar
import kotlin.random.Random

class DailySyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                Catalog(app).dailySync(SettingsStore(app))
            } catch (_: Throwable) {
            }
            schedule(app)
            pending.finish()
        }.start()
    }

    companion object {
        private const val REQ = 23053

        fun schedule(context: Context) {
            val app = context.applicationContext
            val store = SettingsStore(app)
            val pif = store.getGlobalBool(Keys.PIF_AUTO_UPDATE, true)
            val kb = store.getGlobalBool(Keys.KEYBOX_AUTO_UPDATE, true)
            val am = app.getSystemService(AlarmManager::class.java) ?: return
            val pi = PendingIntent.getBroadcast(
                app,
                REQ,
                Intent(app, DailySyncReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (!pif && !kb) {
                try {
                    am.cancel(pi)
                } catch (_: Throwable) {
                }
                return
            }
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // inexact + jitter: no SCHEDULE_EXACT_ALARM, and not every device hits
            // GitHub at the same local midnight
            cal.add(Calendar.MINUTE, Random.nextInt(0, 30))
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } catch (_: Throwable) {
            }
        }
    }
}
