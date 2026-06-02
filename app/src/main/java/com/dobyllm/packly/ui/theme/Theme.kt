package com.dobyllm.packly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
fun PacklyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = PacklyTypography, content = content)
}
