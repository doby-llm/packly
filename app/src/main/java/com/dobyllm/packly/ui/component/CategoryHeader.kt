package com.dobyllm.packly.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.ui.token.CategoryTokens
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun CategoryHeader(category: PacklyCategory, countLabel: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val token = CategoryTokens.byKey(category.key)
    val headerTintAlpha = if (category.key == "clothing") 0.20f else 0.12f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(token.accent.copy(alpha = headerTintAlpha))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        Icon(CategoryTokens.icon(category.iconKey), contentDescription = null, tint = token.accent)
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        CategoryCountPill(countLabel)
    }
}

@Composable
fun CategorySectionCard(
    category: PacklyCategory,
    countLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.xl),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            CategoryHeader(category = category, countLabel = countLabel)
            content()
        }
    }
}

@Composable
fun CategoryCountPill(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PacklyRadius.full),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow
    val foreground = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PacklyRadius.full))
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(PacklyRadius.full))
            .clickable(onClick = onClick)
            .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

@Composable
fun CategoryRowsContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        content()
    }
}

@Composable
fun CategoryRowDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    )
}
