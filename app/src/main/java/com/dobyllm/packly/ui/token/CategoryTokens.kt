package com.dobyllm.packly.ui.token

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Soap
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private fun color(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

data class CategoryToken(
    val key: String,
    val label: String,
    val iconKey: String,
    val accentHex: String,
    val softHex: String,
) {
    val accent: Color get() = color(accentHex)
    val soft: Color get() = color(softHex)
}

object CategoryTokens {
    private const val MINT = "#00e5bc"
    private const val LAVENDER = "#9c7afe"
    private const val SKY = "#94d2ff"
    private const val PEACH = "#ffdad6"
    private const val PRIMARY_FIXED = "#42fdd3"
    private const val SECONDARY_FIXED = "#e8ddff"
    private const val TERTIARY_FIXED = "#c9e6ff"

    // Category accents mirror the Items references: Electronics/mint, Clothing/lavender, Toiletries/sky.
    val all = listOf(
        CategoryToken("electronics", "Electronics", "devices", MINT, MINT),
        CategoryToken("clothing", "Clothing", "checkroom", LAVENDER, LAVENDER),
        CategoryToken("toiletries", "Toiletries", "soap", SKY, SKY),
        CategoryToken("documents", "Documents", "article", "#6844c7", SECONDARY_FIXED),
        CategoryToken("health", "Health", "medical_services", "#ba1a1a", PEACH),
        CategoryToken("comfort", "Travel Comfort", "flight_takeoff", "#006b57", PRIMARY_FIXED),
        CategoryToken("weather", "Weather", "umbrella", "#006591", TERTIARY_FIXED),
        CategoryToken("family", "Kids / Family", "family_restroom", "#6844c7", SECONDARY_FIXED),
        CategoryToken("food", "Food / Snacks", "lunch_dining", "#006b57", PRIMARY_FIXED),
        CategoryToken("misc", "Miscellaneous", "category", "#3b4a44", "#e1e3e4"),
    )

    fun byKey(key: String) = all.firstOrNull { it.key == key } ?: all.last()

    fun icon(iconKey: String): ImageVector = when (iconKey) {
        "checkroom" -> Icons.Rounded.Checkroom
        "soap" -> Icons.Rounded.Soap
        "devices" -> Icons.Rounded.Devices
        "article" -> Icons.Rounded.Article
        "medical_services" -> Icons.Rounded.MedicalServices
        "flight_takeoff" -> Icons.Rounded.FlightTakeoff
        "umbrella" -> Icons.Rounded.Umbrella
        "family_restroom" -> Icons.Rounded.FamilyRestroom
        "lunch_dining" -> Icons.Rounded.LocalDining
        else -> Icons.Rounded.Category
    }
}
