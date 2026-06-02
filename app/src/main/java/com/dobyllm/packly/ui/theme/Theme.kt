package com.dobyllm.packly.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VibrantMinimalismColors = lightColorScheme(
    primary = PacklyPrimary,
    onPrimary = PacklyOnPrimary,
    primaryContainer = PacklyPrimaryContainer,
    onPrimaryContainer = PacklyOnPrimaryContainer,
    inversePrimary = PacklyInversePrimary,
    secondary = PacklySecondary,
    onSecondary = PacklyOnSecondary,
    secondaryContainer = PacklySecondaryContainer,
    onSecondaryContainer = PacklyOnSecondaryContainer,
    tertiary = PacklyTertiary,
    onTertiary = PacklyOnTertiary,
    tertiaryContainer = PacklyTertiaryContainer,
    onTertiaryContainer = PacklyOnTertiaryContainer,
    background = PacklyBackground,
    onBackground = PacklyOnBackground,
    surface = PacklySurface,
    onSurface = PacklyOnSurface,
    surfaceVariant = PacklySurfaceVariant,
    onSurfaceVariant = PacklyOnSurfaceVariant,
    surfaceTint = PacklySurfaceTint,
    inverseSurface = PacklyInverseSurface,
    inverseOnSurface = PacklyInverseOnSurface,
    error = PacklyError,
    onError = PacklyOnError,
    errorContainer = PacklyErrorContainer,
    onErrorContainer = PacklyOnErrorContainer,
    outline = PacklyOutline,
    outlineVariant = PacklyOutlineVariant,
    surfaceBright = PacklySurfaceBright,
    surfaceDim = PacklySurfaceDim,
    surfaceContainer = PacklySurfaceContainer,
    surfaceContainerHigh = PacklySurfaceContainerHigh,
    surfaceContainerHighest = PacklySurfaceContainerHighest,
    surfaceContainerLow = PacklySurfaceContainerLow,
    surfaceContainerLowest = PacklySurfaceContainerLowest,
    primaryFixed = PacklyPrimaryFixed,
    primaryFixedDim = PacklyPrimaryFixedDim,
    onPrimaryFixed = PacklyOnPrimaryFixed,
    onPrimaryFixedVariant = PacklyOnPrimaryFixedVariant,
    secondaryFixed = PacklySecondaryFixed,
    secondaryFixedDim = PacklySecondaryFixedDim,
    onSecondaryFixed = PacklyOnSecondaryFixed,
    onSecondaryFixedVariant = PacklyOnSecondaryFixedVariant,
    tertiaryFixed = PacklyTertiaryFixed,
    tertiaryFixedDim = PacklyTertiaryFixedDim,
    onTertiaryFixed = PacklyOnTertiaryFixed,
    onTertiaryFixedVariant = PacklyOnTertiaryFixedVariant,
)

val PacklyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun PacklyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Strict light-mode brand tokens: user settings are accepted for API compatibility,
    // but dynamic/system colors must not override the Vibrant Minimalism palette.
    @Suppress("UNUSED_VARIABLE")
    val retainedSettings = darkTheme to dynamicColor

    MaterialTheme(
        colorScheme = VibrantMinimalismColors,
        typography = PacklyTypography,
        shapes = PacklyShapes,
        content = content,
    )
}
