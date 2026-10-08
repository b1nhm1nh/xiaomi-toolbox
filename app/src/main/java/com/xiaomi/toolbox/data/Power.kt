package com.xiaomi.toolbox.data

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.widget.Toast

object Power {
    fun reboot(context: Context, reason: String?) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.reboot(reason)
        } catch (t: Throwable) {
            Toast.makeText(context, t.message ?: "reboot failed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Power off via the system shutdown activity. Needs the SHUTDOWN permission
     * (signature|privileged) — silently fails on stock, no reboot fallback by design:
     * PowerManager.shutdown() is hidden API, pm.reboot() cannot power off.
     */
    fun shutdown(context: Context) {
        try {
            context.startActivity(
                Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN")
                    // Intent.EXTRA_KEY_CONFIRM is @hide
                    .putExtra("android.intent.extra.KEY_CONFIRM", false)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (t: Throwable) {
            Toast.makeText(context, t.message ?: "shutdown failed", Toast.LENGTH_SHORT).show()
        }
    }
}
