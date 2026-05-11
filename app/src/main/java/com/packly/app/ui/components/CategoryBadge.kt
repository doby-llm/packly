package com.packly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Category
import com.packly.app.ui.theme.CategoryClothes
import com.packly.app.ui.theme.CategoryShoes
import com.packly.app.ui.theme.CategoryAccessories
import com.packly.app.ui.theme.CategoryTech
import com.packly.app.ui.theme.CategoryToiletries
import com.packly.app.ui.theme.CategoryDocuments
import com.packly.app.ui.theme.CategoryLayers
import com.packly.app.ui.theme.CategoryMisc

fun categoryColor(category: Category): Color = when (category.id) {
    "clothes" -> CategoryClothes
    "shoes" -> CategoryShoes
    "accessories" -> CategoryAccessories
    "technology" -> CategoryTech
    "toiletries" -> CategoryToiletries
    "documents" -> CategoryDocuments
    "extra_layers" -> CategoryLayers
    else -> CategoryMisc
}

fun categoryIcon(category: Category): ImageVector = when (category.id) {
    "clothes" -> Icons.Filled.Checkroom
    "shoes" -> Icons.Filled.Hiking
    "accessories" -> Icons.Filled.Watch
    "technology" -> Icons.Filled.Devices
    "toiletries" -> Icons.Filled.Sanitizer
    "documents" -> Icons.Filled.Description
    "extra_layers" -> Icons.Filled.Umbrella
    else -> Icons.Filled.MoreHoriz
}

@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier
) {
    val color = categoryColor(category)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.name,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
