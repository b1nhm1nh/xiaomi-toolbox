package com.xiaomi.toolbox.data

import org.json.JSONObject

data class TrickStoreTarget(val pkg: String, val flags: Set<TargetFlag>) {
    fun line(): String = pkg + FLAG_ORDER.filter { ch -> flags.any { it.ch == ch } }
        .joinToString("") { it.toString() }

    companion object {
        /** target.txt suffix contract order: leaf, then gen, then PIF. */
        val FLAG_ORDER = listOf('?', '!', '#')
    }
}

object Formats {
    fun parseTargetTxt(raw: String): Map<String, Set<TargetFlag>> {
        if (raw.isBlank()) return emptyMap()
        val out = linkedMapOf<String, Set<TargetFlag>>()
        for (line in raw.lineSequence()) {
            val s = line.trim()
            if (s.isEmpty()) continue
            var end = s.length
            val flags = linkedSetOf<TargetFlag>()
            while (end > 0) {
                val c = s[end - 1]
                val f = TargetFlag.entries.firstOrNull { it.ch == c } ?: break
                flags.add(f)
                end--
            }
            val pkg = s.substring(0, end).trim()
            if (pkg.isNotEmpty()) out[pkg] = flags
        }
        return out
    }

    fun serializeTargetTxt(map: Map<String, Set<TargetFlag>>): String =
        map.entries.joinToString("\n") { TrickStoreTarget(it.key, it.value).line() }

    fun defaultTargetSeed(gmsInstalled: Boolean, vendingInstalled: Boolean): String {
        val leafPif = setOf(TargetFlag.LEAF, TargetFlag.PIF)
        val m = linkedMapOf<String, Set<TargetFlag>>()
        if (gmsInstalled) m["com.google.android.gms"] = leafPif
        if (vendingInstalled) m["com.android.vending"] = leafPif
        return serializeTargetTxt(m)
    }

    fun parsePif(raw: String): LinkedHashMap<String, String> {
        val out = linkedMapOf<String, String>()
        if (raw.isBlank()) return out
        return try {
            val o = JSONObject(raw)
            for (k in Keys.PIF_KNOWN_KEYS) {
                if (o.has(k)) out[k] = o.optString(k)
            }
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (!out.containsKey(k)) out[k] = o.optString(k)
            }
            out
        } catch (_: Throwable) {
            out
        }
    }

    fun serializePif(map: Map<String, String>): String {
        val o = JSONObject()
        for ((k, v) in map) o.put(k, v)
        return o.toString(2)
    }

    fun squeezePatch(securityPatch: String): String {
        val digits = securityPatch.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        // ROM wants yyyyMMdd; "2025-06" -> "20250600" keeps the month, day zero-padded
        return digits.padEnd(8, '0').take(8)
    }

    data class GameProp(
        val brand: String,
        val device: String,
        val manufacturer: String,
        val model: String,
        val fingerprint: String,
        val product: String,
        val name: String = "",
    )

    fun parseGameProps(raw: String): Map<String, GameProp> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val games = root.optJSONObject("games") ?: root
            val out = linkedMapOf<String, GameProp>()
            val keys = games.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val o = games.optJSONObject(pkg) ?: continue
                out[pkg] = GameProp(
                    brand = o.optString("BRAND"),
                    device = o.optString("DEVICE"),
                    manufacturer = o.optString("MANUFACTURER"),
                    model = o.optString("MODEL"),
                    fingerprint = o.optString("FINGERPRINT"),
                    product = o.optString("PRODUCT"),
                    name = o.optString("NAME"),
                )
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun serializeGameProps(map: Map<String, GameProp>): String {
        val games = JSONObject()
        for ((pkg, p) in map) {
            val o = JSONObject()
            // omit blank fields so ROM-side readers don't overwrite props with ""
            if (p.brand.isNotBlank()) o.put("BRAND", p.brand)
            if (p.device.isNotBlank()) o.put("DEVICE", p.device)
            if (p.manufacturer.isNotBlank()) o.put("MANUFACTURER", p.manufacturer)
            if (p.model.isNotBlank()) o.put("MODEL", p.model)
            if (p.fingerprint.isNotBlank()) o.put("FINGERPRINT", p.fingerprint)
            if (p.product.isNotBlank()) o.put("PRODUCT", p.product)
            if (p.name.isNotBlank()) o.put("NAME", p.name)
            games.put(pkg, o)
        }
        return JSONObject().put("games", games).toString(2)
    }

    fun parseAppPropsCatalog(raw: String): Map<String, GameProp> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = linkedMapOf<String, GameProp>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val o = root.optJSONObject(pkg) ?: continue
                out[pkg] = GameProp(
                    brand = o.optString("BRAND"),
                    device = o.optString("DEVICE"),
                    manufacturer = o.optString("MANUFACTURER"),
                    model = o.optString("MODEL"),
                    fingerprint = o.optString("FINGERPRINT"),
                    product = o.optString("PRODUCT"),
                    name = o.optString("NAME"),
                )
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    data class HideDev(val hideDev: Boolean, val hideApp: Boolean)

    fun parseHideDev(raw: String): Map<String, HideDev> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val o = JSONObject(raw)
            val out = linkedMapOf<String, HideDev>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                // one malformed entry must not wipe the whole map
                val e = o.optJSONObject(pkg) ?: continue
                out[pkg] = HideDev(
                    hideDev = e.optBoolean("hidedev", true),
                    hideApp = e.optBoolean("hideapp", false),
                )
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun serializeHideDev(map: Map<String, HideDev>): String {
        val o = JSONObject()
        for ((pkg, e) in map) {
            o.put(
                pkg,
                JSONObject().put("hidedev", e.hideDev).put("hideapp", e.hideApp),
            )
        }
        return o.toString(2)
    }

    data class DeviceModel(
        val name: String,
        val brand: String,
        val device: String,
        val manufacturer: String,
        val model: String,
        val fingerprint: String?,
        val product: String,
    )

    fun parseDeviceModels(raw: String): List<DeviceModel> {
        if (raw.isBlank()) return emptyList()
        return try {
            val root = org.json.JSONArray(raw.takeIf { it.trimStart().startsWith("[") } ?: "[]")
            // also accept object map
            if (raw.trimStart().startsWith("{")) {
                val o = JSONObject(raw)
                val list = mutableListOf<DeviceModel>()
                // common shapes: { "devices": [ ... ] } or array of objects
                val arr = o.optJSONArray("devices") ?: o.optJSONArray("data")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        parseDeviceObj(arr.getJSONObject(i))?.let { list.add(it) }
                    }
                    return list
                }
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val e = o.optJSONObject(k) ?: continue
                    parseDeviceObj(e, fallbackName = k)?.let { list.add(it) }
                }
                return list
            }
            val list = mutableListOf<DeviceModel>()
            for (i in 0 until root.length()) {
                parseDeviceObj(root.getJSONObject(i))?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseDeviceObj(o: JSONObject, fallbackName: String? = null): DeviceModel? {
        val name = o.optString("name").ifBlank { o.optString("NAME").ifBlank { fallbackName ?: "" } }
        val brand = o.optString("brand").ifBlank { o.optString("BRAND") }
        val device = o.optString("device").ifBlank { o.optString("DEVICE") }
        val manufacturer = o.optString("manufacturer").ifBlank { o.optString("MANUFACTURER") }
        val model = o.optString("model").ifBlank { o.optString("MODEL") }
        val fingerprint = o.optString("fingerprint").ifBlank { o.optString("FINGERPRINT") }.ifBlank { null }
        val product = o.optString("product").ifBlank { o.optString("PRODUCT") }
        if (brand.isBlank() && device.isBlank() && model.isBlank()) return null
        return DeviceModel(name.ifBlank { model }, brand, device, manufacturer, model, fingerprint, product)
    }

    fun parseClockColor(hex: String): Int {
        val h = hex.trim().removePrefix("#").uppercase()
        return when (h.length) {
            6 -> (0xFF000000.toInt()) or h.toLong(16).toInt()
            8 -> h.toLong(16).toInt()
            else -> 0xFFFFFFFF.toInt()
        }
    }
}
