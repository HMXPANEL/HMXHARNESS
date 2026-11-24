package com.hmx.harness.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSupportTest {
    private val text = settingsTestStrings()

    private fun entry(query: String, minSdk: Int) =
        matchingSettingsEntries(query, text).firstOrNull { it.minSdk == minSdk }

    @Test
    fun `glass controls stay discoverable and gated by Android version`() {
        val frosted = matchingSettingsEntries("frosted glass", text).filter { it.minSdk == 31 }.first()
        val liquid = matchingSettingsEntries("liquid glass", text).filter { it.minSdk == 33 }.first()
        assertTrue(settingsAvailability(frosted, sdk = 31, hasVibrator = true) is SettingsAvailability.Available)
        assertTrue(settingsAvailability(liquid, sdk = 31, hasVibrator = true) is SettingsAvailability.NeedsAndroid)
        assertTrue(settingsAvailability(liquid, sdk = 33, hasVibrator = true) is SettingsAvailability.Available)
    }

    @Test
    fun `Android 8 sees every newer feature as unavailable but still findable`() {
        for ((query, sdk) in listOf("dynamic color" to 31, "live activities" to 36)) {
            val found = entry(query, sdk)
            assertNotNull("$query should stay searchable", found)
            assertTrue(settingsAvailability(found!!, sdk = 26, hasVibrator = true) is SettingsAvailability.NeedsAndroid)
        }
    }

    @Test
    fun `rumble needs a vibration motor rather than a newer Android`() {
        val rumble = matchingSettingsEntries("rumble", text).first { it.needsVibrator }
        assertEquals(SettingsAvailability.NoVibrator, settingsAvailability(rumble, sdk = 26, hasVibrator = false))
        assertEquals(SettingsAvailability.Available, settingsAvailability(rumble, sdk = 26, hasVibrator = true))
    }

    @Test
    fun `release names follow the API level`() {
        assertEquals("8.0", androidVersionName(26))
        assertEquals("12", androidVersionName(31))
        assertEquals("13", androidVersionName(33))
        assertEquals("16", androidVersionName(36))
        assertEquals("17", androidVersionName(37))
    }
}
