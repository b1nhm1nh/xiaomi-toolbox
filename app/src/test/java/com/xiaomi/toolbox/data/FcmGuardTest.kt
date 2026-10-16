package com.xiaomi.toolbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmGuardTest {
    @Test
    fun mergeAddsDefaultsKeepingOrder() {
        val merged = FcmGuard.mergeApps(listOf("com.foo"), emptySet())
        assertEquals(listOf("com.foo", FcmGuard.REQUIRED, FcmGuard.GSF), merged)
    }

    @Test
    fun mergeDedupesAndPreservesUserEntries() {
        val merged = FcmGuard.mergeApps(
            listOf("com.foo", FcmGuard.REQUIRED, "com.bar"),
            setOf(FcmGuard.REQUIRED, "com.baz"),
        )
        assertEquals(listOf("com.foo", FcmGuard.REQUIRED, "com.bar", FcmGuard.GSF, "com.baz"), merged)
    }

    @Test
    fun splitListIgnoresBlanks() {
        assertEquals(listOf("a", "b"), FcmGuard.splitList("a,, b,,"))
        assertEquals(emptyList<String>(), FcmGuard.splitList(""))
    }

    @Test
    fun repairBaseFallsBackToLastGoodOnlyWhenWiped() {
        assertEquals(listOf("a"), FcmGuard.repairBase(listOf("a"), listOf("x", "y")))
        assertEquals(listOf("x", "y"), FcmGuard.repairBase(emptyList(), listOf("x", "y")))
    }

    @Test
    fun autostartClassify() {
        // AppOpsManager.MODE_ALLOWED = 0, MODE_IGNORED = 1
        assertEquals(FcmApps.Autostart.ENABLED, FcmApps.classify(0, 0))
        assertEquals(FcmApps.Autostart.DISABLED, FcmApps.classify(1, 1))
        assertEquals(FcmApps.Autostart.PARTIAL, FcmApps.classify(0, 1))
        assertEquals(FcmApps.Autostart.UNKNOWN, FcmApps.classify(null, 0))
        assertEquals(FcmApps.Autostart.UNKNOWN, FcmApps.classify(3, 3))
    }
}
