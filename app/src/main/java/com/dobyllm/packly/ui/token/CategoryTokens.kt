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

data class CategoryToken(val key: String, val label: String, val iconKey: String, val accentHex: String, val softHex: String) {
    val accent: Color get() = color(accentHex)
    val soft: Color get() = color(softHex)
}

object CategoryTokens {
    val all = listOf(
        CategoryToken("clothing", "Clothing", "checkroom", "#6C63FF", "#ECEAFF"),
        CategoryToken("toiletries", "Toiletries", "soap", "#00A7A7", "#DDF7F6"),
        CategoryToken("electronics", "Electronics", "devices", "#2F6FED", "#DCE8FF"),
        CategoryToken("documents", "Documents", "article", "#B7791F", "#FFF0D2"),
        CategoryToken("health", "Health", "medical_services", "#D94E67", "#FFE1E8"),
        CategoryToken("comfort", "Travel Comfort", "flight_takeoff", "#7C4DFF", "#EEE6FF"),
        CategoryToken("weather", "Weather", "umbrella", "#F59E0B", "#FEF3C7"),
        CategoryToken("family", "Kids / Family", "family_restroom", "#EC4899", "#FCE7F3"),
        CategoryToken("food", "Food / Snacks", "lunch_dining", "#43A047", "#E5F5E8"),
        CategoryToken("misc", "Miscellaneous", "category", "#64748B", "#EEF2F7"),
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
