package com.xiaomi.toolbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatsTest {
    @Test
    fun targetTxtRoundTrip() {
        val raw = "com.google.android.gms?#\ncom.android.vending!"
        val parsed = Formats.parseTargetTxt(raw)
        assertTrue(parsed["com.google.android.gms"]!!.contains(TargetFlag.LEAF))
        assertTrue(parsed["com.google.android.gms"]!!.contains(TargetFlag.PIF))
        assertEquals(setOf(TargetFlag.GEN), parsed["com.android.vending"])
        val back = Formats.serializeTargetTxt(parsed)
        val re = Formats.parseTargetTxt(back)
        assertEquals(parsed.keys, re.keys)
    }

    @Test
    fun targetTxtSuffixOrderIsLeafGenPif() {
        // ROM contract: flags serialize in the fixed order ? ! # regardless of set order
        val line = TrickStoreTarget(
            "com.google.android.gms",
            setOf(TargetFlag.PIF, TargetFlag.GEN, TargetFlag.LEAF),
        ).line()
        assertEquals("com.google.android.gms?!#", line)
    }

    @Test
    fun pifKeepsUnknownKeys() {
        val json = """{"BRAND":"google","CUSTOM":"x"}"""
        val map = Formats.parsePif(json)
        assertEquals("google", map["BRAND"])
        assertEquals("x", map["CUSTOM"])
        assertEquals("20250605", Formats.squeezePatch("2025-06-05"))
        assertEquals("20250600", Formats.squeezePatch("2025-06"))
        assertEquals("", Formats.squeezePatch("no digits"))
    }

    @Test
    fun gamePropsWrapsGames() {
        val m = mapOf(
            "com.foo" to Formats.GameProp("b", "d", "m", "mo", "fp", "p", "n"),
        )
        val s = Formats.serializeGameProps(m)
        assertTrue(s.contains("\"games\""))
        val parsed = Formats.parseGameProps(s)
        assertEquals("b", parsed["com.foo"]?.brand)
        assertEquals("n", parsed["com.foo"]?.name)
    }

    @Test
    fun gamePropsOmitBlankFields() {
        // blank props must not reach the ROM as "" overrides
        val m = mapOf(
            "com.foo" to Formats.GameProp("b", "", "", "mo", "", ""),
        )
        val s = Formats.serializeGameProps(m)
        assertTrue(s.contains("BRAND"))
        assertTrue(!s.contains("DEVICE"))
        assertTrue(!s.contains("FINGERPRINT"))
        assertEquals("b", Formats.parseGameProps(s)["com.foo"]?.brand)
        assertEquals("", Formats.parseGameProps(s)["com.foo"]?.device)
    }

    @Test
    fun hideDevDefaults() {
        val s = """{"com.bank":{"hidedev":false}}"""
        val m = Formats.parseHideDev(s)
        assertEquals(false, m["com.bank"]?.hideDev)
        assertEquals(false, m["com.bank"]?.hideApp)
    }

    @Test
    fun hideDevToleratesBadEntry() {
        // one malformed entry must not wipe the whole list
        val s = """{"com.bank":{"hidedev":false},"com.broken":"junk"}"""
        val m = Formats.parseHideDev(s)
        assertEquals(false, m["com.bank"]?.hideDev)
        assertEquals(true, m.containsKey("com.broken") == false)
    }
}
