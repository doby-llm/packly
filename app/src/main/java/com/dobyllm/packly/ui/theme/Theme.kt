package com.dobyllm.packly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = PacklyOcean,
    onPrimary = PacklySurface,
    primaryContainer = PacklyOceanSoft,
    onPrimaryContainer = OnPacklyOceanSoft,
    secondary = PacklyMint,
    secondaryContainer = PacklyMintSoft,
    tertiary = PacklyCoral,
    tertiaryContainer = PacklyCoralSoft,
    background = PacklyCanvas,
    surface = PacklySurface,
    surfaceVariant = PacklySurfaceTint,
    outline = PacklyOutline,
    error = PacklyError,
)

private val DarkColors = darkColorScheme(
    primary = PacklyOceanSoft,
    primaryContainer = PacklyOcean,
    secondary = PacklyMint,
    tertiary = PacklyCoral,
    background = PacklyNight,
    surface = PacklyNightSurface,
    surfaceVariant = PacklyNightVariant,
    outline = PacklyNightOutline,
)

@Composable
fun PacklyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PacklyTypography,
        content = content,
    )
}
