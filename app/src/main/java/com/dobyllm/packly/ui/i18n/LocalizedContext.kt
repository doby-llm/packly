package com.dobyllm.packly.ui.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.dobyllm.packly.core.i18n.toLocale
import com.dobyllm.packly.core.model.LanguagePreference
import java.util.Locale

@Composable
fun PacklyLocalizedContent(
    languagePreference: LanguagePreference,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val locale = remember(languagePreference, systemConfiguration) {
        languagePreference.toLocale(systemConfiguration.locales[0] ?: Locale.getDefault())
    }
    val localizedContext = remember(baseContext, locale) { baseContext.withLocale(locale) }
    val localizedConfiguration = remember(systemConfiguration, locale) {
        Configuration(systemConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    SideEffect { Locale.setDefault(locale) }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
    ) {
        content()
    }
}

private fun Context.withLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return createConfigurationContext(configuration)
}
