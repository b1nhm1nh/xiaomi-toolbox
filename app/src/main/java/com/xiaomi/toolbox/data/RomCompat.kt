package com.xiaomi.toolbox.data

/**
 * Detects whether this HyperOS build carries the ROM-side consumers for the
 * kaorios_* / projectzk_* keys (KaoriosHook / ProjectRYUHook smali patches,
 * or a matching version prop). Without them the contract tabs are inert.
 */
object RomCompat {

    data class State(val hooks: Boolean, val version: String)

    @Volatile
    private var cached: State? = null

    fun detect(store: SettingsStore): State {
        cached?.let { return it }
        val state = detectInternal(store)
        cached = state
        return state
    }

    private fun detectInternal(store: SettingsStore): State {
        for (cls in HOOK_CLASSES) {
            try {
                Class.forName(cls)
                return State(true, version(store))
            } catch (_: Throwable) {
            }
        }
        val v = version(store)
        return State(v.isNotBlank(), v)
    }

    private fun version(store: SettingsStore): String =
        store.sysProp("persist.sys.zk.version")
            .ifBlank { store.sysProp("persist.sys.projectryu.version") }
            .ifBlank { store.sysProp("persist.sys.kaorios.version") }

    private val HOOK_CLASSES = listOf(
        "android.security.kaorios.KaoriosHook",
        "android.security.projectryu.ProjectRYUHook",
    )
}
