package com.xiaomi.toolbox.data

import android.content.Context
import android.content.Intent

/**
 * Keeps push-critical apps inside HyperOS' MILLET_NO_RESTRICT_APP background
 * list so their long connections survive PowerKeeper. GMS is always required;
 * the user can protect additional apps (FcmCard → "Protect more apps").
 *
 * Mechanism documented by HyperOS FCM Fix (dingwen07) and FCMGuard-HyperOS (MIT).
 */
object FcmGuard {
    const val KEY = "MILLET_NO_RESTRICT_APP"
    const val REQUIRED = "com.google.android.gms"
    const val GSF = "com.google.android.gsf"

    private const val PREFS = "fcm_guard"
    private const val PREF_EXTRA = "extra_apps"

    data class Status(val present: Boolean, val raw: String, val count: Int)

    fun status(store: SettingsStore): Status {
        val raw = store.getSystemString(KEY, "")
        val items = splitList(raw)
        return Status(REQUIRED in items, raw, items.size)
    }

    /** User-chosen additional protected packages (excluding the GMS/GSF defaults). */
    fun extras(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(PREF_EXTRA, emptySet()) ?: emptySet()

    fun setExtras(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(PREF_EXTRA, pkgs).apply()
    }

    /** Existing list + defaults + extras, deduped, order preserved. Pure — unit-tested. */
    fun mergeApps(existing: List<String>, extras: Collection<String>): List<String> =
        (existing + REQUIRED + GSF + extras).distinct()

    /**
     * Appends the required/extra apps to the MILLET list if missing.
     * Returns true only when a write happened and succeeded.
     */
    fun repair(context: Context, store: SettingsStore): Boolean {
        val raw = store.getSystemString(KEY, "")
        val items = splitList(raw)
        val want = mergeApps(items, extras(context))
        if (want == items) return false
        return store.putSystemString(KEY, want.joinToString(","), silent = true)
    }

    /** Best-effort FCM/MCS heartbeat kick, mirroring HeartbeatFixerForFCM. */
    fun kick(context: Context): Boolean {
        var sent = false
        for (target in listOf(GSF, REQUIRED)) {
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
        val repaired = repair(context, store)
        if (repaired) kick(context)
        return repaired
    }

    fun splitList(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
