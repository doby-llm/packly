package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { System, Light, Dark }

@Serializable
data class PacklySettings(
    val themeMode: ThemeMode = ThemeMode.Light,
    val dynamicColorEnabled: Boolean = false,
    val selectedPaletteKey: String = "packly_default",
    val firstLaunchCompleted: Boolean = false,
)

@Serializable
data class PacklySessionState(
    val collapsedCategoryKeysByScreen: Map<String, Set<String>> = emptyMap(),
    val lastUsedCategoryId: CategoryId? = null,
    val lastOpenedTripId: TripId? = null,
)
