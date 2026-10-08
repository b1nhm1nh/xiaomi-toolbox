package com.xiaomi.toolbox.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class Catalog(private val context: Context) {

    fun fetch(name: String): String? {
        val url = Keys.CATALOG_BASE + name
        return try {
            httpGet(url)
        } catch (_: Throwable) {
            null
        }
    }

    fun httpGet(url: String, capBytes: Int = MAX_BODY): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        conn.setRequestProperty("User-Agent", Keys.USER_AGENT)
        conn.instanceFollowRedirects = true
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { r ->
                val buf = CharArray(8192)
                var n: Int
                var total = 0
                while (r.read(buf).also { n = it } > 0) {
                    total += n
                    if (total > capBytes) error("body too large")
                    sb.append(buf, 0, n)
                }
            }
            return sb.toString()
        } finally {
            conn.disconnect()
        }
    }

    fun cacheFile(name: String): java.io.File = java.io.File(context.filesDir, name)

    fun readCache(name: String): String? {
        val f = cacheFile(name)
        return if (f.exists()) f.readText() else null
    }

    fun writeCache(name: String, body: String) {
        cacheFile(name).writeText(body)
    }

    /** User-imported keyboxes are never overwritten by the catalog. */
    fun markKeyboxImported() {
        prefs().edit().putBoolean("keybox_imported", true).apply()
    }

    private fun isKeyboxImported(): Boolean = prefs().getBoolean("keybox_imported", false)

    private fun prefs() = context.getSharedPreferences("sync_state", Context.MODE_PRIVATE)

    fun refreshKeybox(store: SettingsStore): Boolean {
        if (isKeyboxImported()) return false
        val body = fetch("Keybox.xml") ?: return false
        // Never auto-apply a catalog keybox Google has already revoked.
        val serials = Revocation.parseSerials(body)
        if (serials.isNotEmpty()) {
            val statuses = try {
                Revocation.fetchStatuses()
            } catch (_: Throwable) {
                null
            }
            if (statuses != null) {
                val affected = serials.filter {
                    statuses[it]?.equals(Revocation.STATE_REVOKED, true) == true
                }
                if (affected.isNotEmpty()) {
                    Revocation.saveVerdict(
                        context,
                        Revocation.STATE_REVOKED,
                        serials.size,
                        affected,
                        serials,
                    )
                    return false
                }
            }
        }
        return store.putGlobal(Keys.KEYBOX_XML, body, silent = true)
    }

    fun refreshPif(store: SettingsStore): Boolean {
        val body = fetch("Pif-props.json") ?: return false
        return applyPif(store, body)
    }

    /** Single path for PIF application (catalog + manual import): writes PIF + patch. */
    fun applyPif(store: SettingsStore, body: String): Boolean {
        if (Formats.parsePif(body).isEmpty()) return false
        val ok = store.putGlobal(Keys.PIF_JSON, body, silent = true)
        if (ok) {
            val map = Formats.parsePif(body)
            val patch = Formats.squeezePatch(map["SECURITY_PATCH"].orEmpty())
            if (patch.length == 8) store.putGlobal(Keys.SECURITY_PATCH, patch, silent = true)
        }
        return ok
    }

    fun refreshDeviceModels(): List<Formats.DeviceModel> {
        val body = fetch("device-model.json")
        if (body != null) {
            writeCache("device_models_cache.json", body)
            return Formats.parseDeviceModels(body)
        }
        return Formats.parseDeviceModels(readCache("device_models_cache.json").orEmpty())
    }

    fun refreshAppProps(): Map<String, Formats.GameProp> {
        val body = fetch("app-props.json")
        if (body != null) {
            writeCache("app_props_cache.json", body)
            return Formats.parseAppPropsCatalog(body)
        }
        return Formats.parseAppPropsCatalog(readCache("app_props_cache.json").orEmpty())
    }

    /** Catalog publish stamp (date-update.txt, ddMMyyyy). */
    fun refreshDate(): String? {
        val body = fetch("date-update.txt") ?: return catalogDate()
        val trimmed = body.trim()
        if (trimmed.isNotEmpty()) writeCache("date_cache.txt", trimmed)
        return catalogDate()
    }

    fun catalogDate(): String? {
        val raw = readCache("date_cache.txt")?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return try {
            val d = java.text.SimpleDateFormat("ddMMyyyy", java.util.Locale.US).parse(raw)
            java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(d!!)
        } catch (_: Throwable) {
            raw
        }
    }

    /** Returns true when every attempted refresh succeeded. */
    fun dailySync(store: SettingsStore): Boolean {
        refreshDate()
        if (!RomCompat.detect(store).hooks) return true   // nothing to sync into on stock
        val pif = store.getGlobalBool(Keys.PIF_AUTO_UPDATE, true)
        val kb = store.getGlobalBool(Keys.KEYBOX_AUTO_UPDATE, true)
        if (!pif && !kb) return true
        val prefs = context.getSharedPreferences(Keys.PREF_SYNC, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        var ok = true
        if (kb) ok = refreshKeybox(store) && ok
        if (pif) ok = refreshPif(store) && ok
        ok = try { refreshDeviceModels(); true } catch (_: Throwable) { false } && ok
        if (ok) {
            prefs.edit().putString("last_day", today).apply()
        }
        return ok
    }

    companion object {
        const val MAX_BODY = 256 * 1024
    }
}
