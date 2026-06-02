package com.dobyllm.packly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dobyllm.packly.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_400, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_800, FontWeight.ExtraBold),
)

private fun packlyTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val PacklyTypography = Typography(
    displayLarge = packlyTextStyle(48, 56, FontWeight.ExtraBold, -0.96f),
    displayMedium = packlyTextStyle(48, 56, FontWeight.ExtraBold, -0.96f),
    displaySmall = packlyTextStyle(32, 40, FontWeight.Bold, -0.32f),
    headlineLarge = packlyTextStyle(32, 40, FontWeight.Bold, -0.32f),
    headlineMedium = packlyTextStyle(28, 36, FontWeight.Bold),
    headlineSmall = packlyTextStyle(24, 32, FontWeight.Bold),
    titleLarge = packlyTextStyle(20, 28, FontWeight.SemiBold),
    titleMedium = packlyTextStyle(20, 28, FontWeight.SemiBold),
    titleSmall = packlyTextStyle(16, 24, FontWeight.SemiBold),
    bodyLarge = packlyTextStyle(16, 24, FontWeight.Normal),
    bodyMedium = packlyTextStyle(16, 24, FontWeight.Normal),
    bodySmall = packlyTextStyle(14, 20, FontWeight.Normal),
    labelLarge = packlyTextStyle(14, 20, FontWeight.SemiBold, 0.7f),
    labelMedium = packlyTextStyle(12, 16, FontWeight.SemiBold, 0.6f),
    labelSmall = packlyTextStyle(12, 16, FontWeight.SemiBold, 0.6f),
)
