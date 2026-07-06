package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { System, Light, Dark }

@Serializable
enum class LanguagePreference { System, English, Spanish, German }

@Serializable
data class PacklySettings(
    val themeMode: ThemeMode = ThemeMode.Light,
    val dynamicColorEnabled: Boolean = false,
    val selectedPaletteKey: String = "packly_default",
    val firstLaunchCompleted: Boolean = false,
    val languagePreference: LanguagePreference = LanguagePreference.System,
    val cloudSync: PacklyCloudSyncSettings = PacklyCloudSyncSettings(),
)

@Serializable
data class PacklySessionState(
    val collapsedCategoryKeysByScreen: Map<String, Set<String>> = emptyMap(),
    val lastUsedCategoryId: CategoryId? = null,
    val lastOpenedTripId: TripId? = null,
)
