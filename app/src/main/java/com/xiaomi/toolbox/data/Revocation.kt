package com.xiaomi.toolbox.data

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Keybox revocation check against Google's public attestation status list.
 * Verdicts are cached so an offline launch still shows the last known state.
 */
object Revocation {

    const val STATUS_URL = "https://android.googleapis.com/attestation/status"

    const val STATE_EMPTY = "EMPTY"
    const val STATE_VALID = "VALID"
    const val STATE_SUSPENDED = "SUSPENDED"
    const val STATE_REVOKED = "REVOKED"
    const val STATE_OFFLINE = "OFFLINE"

    data class Result(
        val state: String,
        val checkedAt: Long,
        val checkedSerials: Int,
        val affectedSerials: List<String>,
        val serials: List<String> = emptyList(),
    )

    /** Lowercase hex serials of every certificate in the keybox XML, no leading zeros. */
    fun parseSerials(keyboxXml: String): List<String> {
        if (keyboxXml.isBlank()) return emptyList()
        val certRegex = Regex("(?is)<Certificate(?:\\s[^>]*)?>(.*?)</Certificate>")
        val factory = CertificateFactory.getInstance("X.509")
        val serials = linkedSetOf<String>()
        for (m in certRegex.findAll(keyboxXml)) {
            val pem = m.groupValues[1]
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\\s".toRegex(), "")
            if (pem.isEmpty()) continue
            try {
                val der = Base64.decode(pem, Base64.DEFAULT)
                val cert = factory.generateCertificate(der.inputStream()) as X509Certificate
                serials += cert.serialNumber.toString(16).lowercase()
            } catch (_: Throwable) {
                // skip malformed entry, keep the rest
            }
        }
        return serials.toList()
    }

    /** Worst status across the keybox's certificates, per Google's status list. */
    fun check(context: Context, keyboxXml: String): Result {
        val serials = parseSerials(keyboxXml)
        if (serials.isEmpty()) {
            return save(context, Result(STATE_EMPTY, System.currentTimeMillis(), 0, emptyList()))
        }
        val statuses = try {
            fetchStatuses()
        } catch (_: Throwable) {
            // Offline: reuse the cached verdict ONLY if it was for the same keybox.
            val cached = cached(context)?.takeIf { it.serials.toSet() == serials.toSet() }
            val state = if (cached != null && cached.state != STATE_OFFLINE) cached.state else STATE_OFFLINE
            return save(
                context,
                Result(
                    state,
                    cached?.checkedAt ?: 0L,
                    serials.size,
                    cached?.affectedSerials ?: emptyList(),
                    serials,
                ),
            )
        }
        val affected = serials.filter { statuses[it]?.equals(STATE_REVOKED, true) == true }
        val suspended = serials.filter { statuses[it]?.equals(STATE_SUSPENDED, true) == true }
        val state = when {
            affected.isNotEmpty() -> STATE_REVOKED
            suspended.isNotEmpty() -> STATE_SUSPENDED
            else -> STATE_VALID
        }
        return save(context, Result(state, System.currentTimeMillis(), serials.size, affected + suspended, serials))
    }

    /** Store a verdict computed elsewhere (e.g. catalog pre-check). */
    fun saveVerdict(
        context: Context,
        state: String,
        serialCount: Int,
        affected: List<String>,
        serials: List<String>,
    ): Result = save(context, Result(state, System.currentTimeMillis(), serialCount, affected, serials))

    fun cached(context: Context): Result? {
        val raw = try {
            java.io.File(context.filesDir, "keybox_revocation_cache.json").readText()
        } catch (_: Throwable) {
            return null
        }
        return try {
            val o = JSONObject(raw)
            fun list(name: String): List<String> {
                val arr = o.optJSONArray(name) ?: return emptyList()
                return (0 until arr.length()).map { arr.optString(it) }
            }
            Result(
                o.optString("state", STATE_OFFLINE),
                o.optLong("checked_at", 0L),
                o.optInt("checked_serials", 0),
                list("affected"),
                list("serials"),
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun save(context: Context, r: Result): Result {
        try {
            val o = JSONObject()
                .put("state", r.state)
                .put("checked_at", r.checkedAt)
                .put("checked_serials", r.checkedSerials)
                .put("affected", org.json.JSONArray(r.affectedSerials))
                .put("serials", org.json.JSONArray(r.serials))
            java.io.File(context.filesDir, "keybox_revocation_cache.json").writeText(o.toString(2))
        } catch (_: Throwable) {
        }
        return r
    }

    /** serial -> status ("REVOKED" / "SUSPENDED" / "VALID"). */
    fun fetchStatuses(): Map<String, String> {
        val conn = URL(STATUS_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", Keys.USER_AGENT)
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val entries = JSONObject(body).optJSONObject("entries") ?: JSONObject()
            val map = linkedMapOf<String, String>()
            for (key in entries.keys()) {
                map[key.lowercase()] = entries.optJSONObject(key)?.optString("status", "VALID") ?: "VALID"
            }
            return map
        } finally {
            conn.disconnect()
        }
    }
}
