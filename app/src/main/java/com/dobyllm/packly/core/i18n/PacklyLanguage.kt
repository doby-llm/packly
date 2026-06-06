package com.dobyllm.packly.core.i18n

import com.dobyllm.packly.core.model.LanguagePreference
import java.util.Locale

private val SupportedLocales = mapOf(
    LanguagePreference.English to Locale.forLanguageTag("en"),
    LanguagePreference.Spanish to Locale.forLanguageTag("es"),
    LanguagePreference.German to Locale.forLanguageTag("de"),
)

fun LanguagePreference.toLocale(systemLocale: Locale = Locale.getDefault()): Locale = when (this) {
    LanguagePreference.System -> systemLocale.supportedPacklyLocale()
    else -> SupportedLocales.getValue(this)
}

fun Locale.supportedPacklyLocale(): Locale = when (language.lowercase(Locale.ROOT)) {
    "es" -> SupportedLocales.getValue(LanguagePreference.Spanish)
    "de" -> SupportedLocales.getValue(LanguagePreference.German)
    else -> SupportedLocales.getValue(LanguagePreference.English)
}
