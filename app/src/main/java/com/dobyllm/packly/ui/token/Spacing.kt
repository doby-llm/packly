package com.dobyllm.packly.ui.token

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PacklySpacing {
    val xs = 4.dp
    val base = 8.dp
    val sm = 12.dp
    val marginMobile = 16.dp
    val md = 24.dp
    val gutter = 24.dp
    val lg = 48.dp
    val xl = 80.dp

    // Backwards-compatible aliases for existing screens until feature-level redesigns land.
    val tiny: Dp = xs
    val small: Dp = base
    val medium: Dp = sm
    val large: Dp = marginMobile
    val xlarge: Dp = md
    val xxlarge: Dp = lg
}

object PacklyRadius {
    val sm = 4.dp
    val default = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 9999.dp
}

object PacklyElevation {
    val floor = 0.dp
    val card = 2.dp
    val floating = 8.dp
    val overlayBlur = 12.dp
}
