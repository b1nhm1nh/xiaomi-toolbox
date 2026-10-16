package com.xiaomi.toolbox.data

import android.content.Context
import android.content.Intent

/**
 * Keeps push-critical apps inside HyperOS' MILLET_NO_RESTRICT_APP background
 * list so their long connections survive PowerKeeper. GMS is always required;
 * the user can protect additional apps (FcmCard → "Protect more apps").
 *
 * MILLET_NO_RESTRICT_APP is a private Settings.System key: SettingsProvider only
 * lets non-privileged apps write it when they target SDK ≤ 22, which is why the
 * app keeps targetSdk 22 (no root / Shizuku needed, just WRITE_SETTINGS).
 *
 * Mechanism documented by HyperOS FCM Fix (dingwen07) and FCMGuard-HyperOS (MIT).
 */
object FcmGuard {
    const val KEY = "MILLET_NO_RESTRICT_APP"
    const val REQUIRED = "com.google.android.gms"
    const val GSF = "com.google.android.gsf"

    private const val PREFS = "fcm_guard"
    private const val PREF_EXTRA = "extra_apps"
    private const val PREF_AUTO = "auto_protect"
    private const val PREF_PERSISTENT = "persistent_notification"
    private const val PREF_LAST_GOOD = "last_good"

    data class Status(val present: Boolean, val raw: String, val count: Int)

    fun status(store: SettingsStore): Status {
        val raw = store.getSystemString(KEY, "")
        val items = splitList(raw)
        return Status(REQUIRED in items, raw, items.size)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** User-chosen additional protected packages (excluding the GMS/GSF defaults). */
    fun extras(context: Context): Set<String> =
        prefs(context).getStringSet(PREF_EXTRA, emptySet()) ?: emptySet()

    fun setExtras(context: Context, pkgs: Set<String>) {
        prefs(context).edit().putStringSet(PREF_EXTRA, pkgs).apply()
    }

    /** Watchdog service (observer + 30 min fallback) runs while this is on. */
    fun isAutoEnabled(context: Context): Boolean = prefs(context).getBoolean(PREF_AUTO, false)

    fun setAutoEnabled(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(PREF_AUTO, on).apply()
    }

    /** Foreground (ongoing notification) vs quiet background service. */
    fun usePersistentNotification(context: Context): Boolean =
        prefs(context).getBoolean(PREF_PERSISTENT, true)

    fun setPersistentNotification(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(PREF_PERSISTENT, on).apply()
    }

    /** Existing list + defaults + extras, deduped, order preserved. Pure — unit-tested. */
    fun mergeApps(existing: List<String>, extras: Collection<String>): List<String> =
        (existing + REQUIRED + GSF + extras).distinct()

    /**
     * Base list to repair from. HyperOS sometimes wipes the key entirely; then the
     * last list we saw with content is restored instead of only re-adding ours.
     * Pure — unit-tested.
     */
    fun repairBase(current: List<String>, lastGood: List<String>): List<String> =
        current.ifEmpty { lastGood }

    /**
     * Appends the required/extra apps to the MILLET list if missing.
     * Returns true only when a write happened and succeeded.
     */
    fun repair(context: Context, store: SettingsStore): Boolean {
        val items = splitList(store.getSystemString(KEY, ""))
        val lastGood = splitList(prefs(context).getString(PREF_LAST_GOOD, "") ?: "")
        val want = mergeApps(repairBase(items, lastGood), extras(context))
        if (want == items) {
            rememberLastGood(context, items)
            return false
        }
        val ok = store.putSystemString(KEY, want.joinToString(","), silent = true)
        if (ok) rememberLastGood(context, want)
        return ok
    }

    private fun rememberLastGood(context: Context, items: List<String>) {
        if (items.isEmpty()) return
        val joined = items.joinToString(",")
        val p = prefs(context)
        if (p.getString(PREF_LAST_GOOD, null) != joined) p.edit().putString(PREF_LAST_GOOD, joined).apply()
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
