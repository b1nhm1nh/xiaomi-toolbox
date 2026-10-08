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
}
