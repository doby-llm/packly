package com.dobyllm.packly.core.time

import java.time.ZoneId
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacklyDeadlineFormatterTest {
    private val originalLocale: Locale = Locale.getDefault()
    private val utc: ZoneId = ZoneId.of("UTC")
    private val deadline = "2026-06-07T12:00:00Z"

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun displayDeadlineUsesEnglishSpanishAndGermanLocaleText() {
        Locale.setDefault(Locale.ENGLISH)
        val english = PacklyDeadlineFormatter.formatDisplay(deadline, utc).orEmpty()

        Locale.setDefault(Locale.forLanguageTag("es"))
        val spanish = PacklyDeadlineFormatter.formatDisplay(deadline, utc).orEmpty()

        Locale.setDefault(Locale.GERMAN)
        val german = PacklyDeadlineFormatter.formatDisplay(deadline, utc).orEmpty()

        assertEquals("Sun, Jun 7, 12:00", english)
        assertEquals("dom, 7 jun, 12:00", spanish)
        assertEquals("So., 7. Juni, 12:00", german)
        assertNoEnglishConnector(spanish)
        assertNoEnglishConnector(german)
    }

    @Test
    fun displayDeadlineReflectsRuntimeLocaleSwitches() {
        Locale.setDefault(Locale.ENGLISH)
        val english = PacklyDeadlineFormatter.formatDisplay(deadline, utc).orEmpty()

        Locale.setDefault(Locale.forLanguageTag("es"))
        val spanishAfterSwitch = PacklyDeadlineFormatter.formatDisplay(deadline, utc).orEmpty()

        assertNotEquals(english, spanishAfterSwitch)
        assertFalse(spanishAfterSwitch.contains("Sun"))
        assertTrue(spanishAfterSwitch.contains("dom"))
        assertNoEnglishConnector(spanishAfterSwitch)
    }

    @Test
    fun displayDeadlineFallsBackToEnglishForUnsupportedLocales() {
        Locale.setDefault(Locale.FRENCH)

        assertEquals("Sun, Jun 7, 12:00", PacklyDeadlineFormatter.formatDisplay(deadline, utc))
    }

    private fun assertNoEnglishConnector(value: String) {
        assertFalse("Expected no English `at` connector in `$value`", value.contains(" at "))
    }
}
