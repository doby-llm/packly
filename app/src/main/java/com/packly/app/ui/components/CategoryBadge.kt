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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Charger
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Shoes
import androidx.compose.material.icons.filled.Soap
import androidx.compose.material.icons.filled.Sunglasses
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
import com.packly.app.model.Category
import com.packly.app.ui.theme.CategoryAccessories
import com.packly.app.ui.theme.CategoryClothing
import com.packly.app.ui.theme.CategoryDocuments
import com.packly.app.ui.theme.CategoryLayers
import com.packly.app.ui.theme.CategoryShoes
import com.packly.app.ui.theme.CategoryTech
import com.packly.app.ui.theme.CategoryToiletries

/**
 * Maps each [Category] to a brand color and a Material icon.
 */
fun categoryColor(category: Category): Color = when (category) {
    Category.CLOTHING_TOP -> CategoryClothing
    Category.CLOTHING_BOTTOM -> CategoryClothing
    Category.SHOES_SOCKS -> CategoryShoes
    Category.EXTRA_LAYERS -> CategoryLayers
    Category.ACCESSORIES -> CategoryAccessories
    Category.TECHNOLOGY -> CategoryTech
    Category.TOILETRIES -> CategoryToiletries
    Category.DOCUMENTS -> CategoryDocuments
}

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.CLOTHING_TOP -> Icons.Default.Checkroom
    Category.CLOTHING_BOTTOM -> Icons.Default.Hiking
    Category.SHOES_SOCKS -> Icons.Default.Shoes
    Category.EXTRA_LAYERS -> Icons.Default.Checkroom
    Category.ACCESSORIES -> Icons.Default.Sunglasses
    Category.TECHNOLOGY -> Icons.Default.Charger
    Category.TOILETRIES -> Icons.Default.Soap
    Category.DOCUMENTS -> Icons.Default.Article
}

/**
 * A small colored chip showing category icon + name.
 * Used inline in packing item rows.
 */
@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
) {
    val color = categoryColor(category)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.displayName,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * A larger circle icon badge — used on the trip detail header.
 */
@Composable
fun CategoryDot(
    category: Category,
    modifier: Modifier = Modifier,
) {
    val color = categoryColor(category)
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.displayName,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}
