package com.xiaomi.toolbox.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import com.xiaomi.toolbox.R

class SettingsStore(private val context: Context) {

    /** Contract keys are inert without ROM hooks — never expose them on stock builds. */
    private val hooks: Boolean by lazy { RomCompat.detect(this).hooks }

    fun canWriteGlobal(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun canWriteSystem(): Boolean = Settings.System.canWrite(context)

    fun getGlobal(key: String, default: String = ""): String =
        try {
            Settings.Global.getString(context.contentResolver, key) ?: default
        } catch (_: Throwable) {
            default
        }

    fun getGlobalBool(key: String, default: Boolean): Boolean {
        val v = getGlobal(key, "")
        if (v.isBlank()) return default
        return v.trim() == "1"
    }

    fun putGlobal(key: String, value: String, silent: Boolean = false): Boolean {
        if (key.startsWith("kaorios_") && !hooks) {
            if (!silent) toast(R.string.rom_hooks_card_note)
            return false
        }
        val ok = try {
            Settings.Global.putString(context.contentResolver, key, value)
            true
        } catch (_: Throwable) {
            false
        }
        if (ok) {
            try {
                Settings.Global.putString(
                    context.contentResolver,
                    Keys.TIME,
                    System.currentTimeMillis().toString(),
                )
            } catch (_: Throwable) {
            }
            if (!silent) toast(R.string.write_ok)
        } else if (!silent) {
            toast(R.string.write_global_fail)
        }
        return ok
    }

    fun putGlobalBool(key: String, value: Boolean, silent: Boolean = false): Boolean =
        putGlobal(key, if (value) "1" else "0", silent)

    fun getSystemInt(key: String, default: Int): Int =
        try {
            Settings.System.getInt(context.contentResolver, key, default)
        } catch (_: Throwable) {
            default
        }

    fun getSystemString(key: String, default: String): String =
        try {
            Settings.System.getString(context.contentResolver, key)?.takeIf { it.isNotBlank() } ?: default
        } catch (_: Throwable) {
            default
        }

    fun putSystemInt(key: String, value: Int, silent: Boolean = false): Boolean {
        if (isContractKey(key) && !hooks) {
            if (!silent) toast(R.string.rom_hooks_card_note)
            return false
        }
        if (!canWriteSystem()) {
            if (!silent) toast(R.string.sys_write_fail)
            return false
        }
        val ok = try {
            Settings.System.putInt(context.contentResolver, key, value)
            true
        } catch (_: Throwable) {
            false
        }
        if (!ok && !silent) toast(R.string.sys_write_fail)
        return ok
    }

    fun putSystemBool(key: String, value: Boolean, silent: Boolean = false): Boolean =
        putSystemInt(key, if (value) 1 else 0, silent)

    fun putSystemString(key: String, value: String?, silent: Boolean = false): Boolean {
        if (isContractKey(key) && !hooks) {
            if (!silent) toast(R.string.rom_hooks_card_note)
            return false
        }
        if (!canWriteSystem()) {
            if (!silent) toast(R.string.sys_write_fail)
            return false
        }
        val ok = try {
            Settings.System.putString(context.contentResolver, key, value)
            true
        } catch (_: Throwable) {
            false
        }
        if (!ok && !silent) toast(R.string.sys_write_fail)
        return ok
    }

    fun getSecureInt(key: String, default: Int): Int =
        try {
            Settings.Secure.getInt(context.contentResolver, key, default)
        } catch (_: Throwable) {
            default
        }

    fun putSecureInt(key: String, value: Int, silent: Boolean = false): Boolean {
        val ok = try {
            Settings.Secure.putInt(context.contentResolver, key, value)
            true
        } catch (_: Throwable) {
            false
        }
        if (!ok && !silent) toast(R.string.sys_write_fail)
        return ok
    }

    fun isContractKey(key: String): Boolean =
        key.startsWith("projectzk_") || key.startsWith("kaorios_") ||
            key == Keys.SHOW_ROTATION || Keys.SYSTEM_KEYS.contains(key)

    fun seedDefaults() {
        if (canWriteGlobal()) {
            for ((k, def) in Keys.GLOBAL_BOOL_DEFAULTS) {
                val cur = getGlobal(k, "")
                if (cur.isBlank()) putGlobal(k, if (def) "1" else "0", silent = true)
            }
            val mode = getGlobal(Keys.KEYBOX_APPLY_ALL_MODE, "")
            if (mode.isBlank() || mode == "all") {
                putGlobal(Keys.KEYBOX_APPLY_ALL_MODE, Keys.APPLY_ALL_MODE_DEFAULT, silent = true)
            }
            val tgt = getGlobal(Keys.TARGET_TXT, "")
            if (tgt.isBlank()) {
                val gms = isInstalled("com.google.android.gms")
                val vending = isInstalled("com.android.vending")
                val seed = Formats.defaultTargetSeed(gms, vending)
                if (seed.isNotBlank()) putGlobal(Keys.TARGET_TXT, seed, silent = true)
            }
        }
        if (canWriteSystem()) {
            val pat = getSystemString(Keys.CLOCK_PATTERN, "")
            if (pat.isBlank()) putSystemString(Keys.CLOCK_PATTERN, Keys.CLOCK_PATTERN_DEFAULT, silent = true)
            val col = getSystemString(Keys.CLOCK_COLOR, "")
            if (col.isBlank()) putSystemString(Keys.CLOCK_COLOR, Keys.CLOCK_COLOR_DEFAULT, silent = true)
            for (k in Keys.SYSTEM_KEYS) {
                if (k == Keys.CLOCK_PATTERN || k == Keys.CLOCK_COLOR || k == Keys.CLOCK_ENABLED) continue
                val cur = getSystemString(k, "")
                if (cur.isBlank()) {
                    val d = Keys.SYSTEM_INT_DEFAULTS[k] ?: 0
                    putSystemInt(k, d, silent = true)
                }
            }
        }
    }

    fun isInstalled(pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Throwable) {
            false
        }

    fun sysProp(name: String): String =
        try {
            val c = Class.forName("android.os.SystemProperties")
            val m = c.getMethod("get", String::class.java, String::class.java)
            (m.invoke(null, name, "") as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }

    private fun toast(res: Int) {
        Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
    }
}
