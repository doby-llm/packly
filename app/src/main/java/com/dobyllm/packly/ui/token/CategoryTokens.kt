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

    // Category accents reuse Vibrant Minimalism containers at soft opacity values per DESIGN.md.
    val all = listOf(
        CategoryToken("clothing", "Clothing", "checkroom", "#006b57", MINT),
        CategoryToken("toiletries", "Toiletries", "soap", "#006b57", MINT),
        CategoryToken("electronics", "Electronics", "devices", "#006591", SKY),
        CategoryToken("documents", "Documents", "article", "#6844c7", LAVENDER),
        CategoryToken("health", "Health", "medical_services", "#ba1a1a", PEACH),
        CategoryToken("comfort", "Travel Comfort", "flight_takeoff", "#6844c7", LAVENDER),
        CategoryToken("weather", "Weather", "umbrella", "#006591", SKY),
        CategoryToken("family", "Kids / Family", "family_restroom", "#6844c7", LAVENDER),
        CategoryToken("food", "Food / Snacks", "lunch_dining", "#006b57", MINT),
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
