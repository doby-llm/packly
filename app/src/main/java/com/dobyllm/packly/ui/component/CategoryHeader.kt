package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.ui.token.CategoryTokens

@Composable
fun CategoryHeader(category: PacklyCategory, countLabel: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val token = CategoryTokens.byKey(category.key)
    Row(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(token.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(CategoryTokens.icon(category.iconKey), contentDescription = null, tint = token.accent)
        }
        Text(category.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(countLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
