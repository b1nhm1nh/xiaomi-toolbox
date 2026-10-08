package com.xiaomi.toolbox.data

import android.content.Context
import android.content.Intent

/**
 * Keeps com.google.android.gms inside HyperOS' MILLET_NO_RESTRICT_APP
 * background list so the FCM/MCS long connection survives PowerKeeper.
 *
 * Mechanism documented by HyperOS FCM Fix (dingwen07) and FCMGuard-HyperOS (MIT).
 */
object FcmGuard {
    const val KEY = "MILLET_NO_RESTRICT_APP"
    const val REQUIRED = "com.google.android.gms"

    data class Status(val present: Boolean, val raw: String, val count: Int)

    fun status(store: SettingsStore): Status {
        val raw = store.getSystemString(KEY, "")
        val items = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return Status(REQUIRED in items, raw, items.size)
    }

    /** Appends GMS to the list if missing. Returns true only when a write happened and succeeded. */
    fun repair(store: SettingsStore): Boolean {
        val raw = store.getSystemString(KEY, "")
        val items = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (REQUIRED in items) return false
        return store.putSystemString(KEY, (items + REQUIRED).joinToString(","), silent = true)
    }

    /** Best-effort FCM/MCS heartbeat kick, mirroring HeartbeatFixerForFCM. */
    fun kick(context: Context): Boolean {
        var sent = false
        for (target in listOf("com.google.android.gms", "com.google.android.gsf")) {
            for (action in listOf(
                "com.google.android.intent.action.GTALK_HEARTBEAT",
                "com.google.android.intent.action.MCS_HEARTBEAT",
            )) {
                try {
                    context.sendBroadcast(Intent(action).setPackage(target))
                    sent = true
                } catch (_: Throwable) {
                }
            }
        }
        return sent
    }

    /** Repair when needed, then kick the transport. Returns the repair result. */
    fun repairAndKick(context: Context, store: SettingsStore): Boolean {
        val repaired = repair(store)
        if (repaired) kick(context)
        return repaired
    }
}
