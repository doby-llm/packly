@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.ui.theme.PacklyOnPrimaryFixedVariant
import com.dobyllm.packly.ui.theme.PacklyOnSecondaryFixedVariant
import com.dobyllm.packly.ui.theme.PacklyOnTertiaryFixedVariant
import com.dobyllm.packly.ui.theme.PacklyPrimaryFixed
import com.dobyllm.packly.ui.theme.PacklySecondaryFixed
import com.dobyllm.packly.ui.theme.PacklySurfaceContainerHigh
import com.dobyllm.packly.ui.theme.PacklyTertiaryFixed
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ListCard(
    list: PacklyList,
    categories: List<PacklyCategory>,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = rememberListAccent(list.name)
    val categoryChips = list.entries
        .sortedBy { it.sortOrder }
        .mapNotNull { entry -> categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.label }
        .distinct()
        .take(3)

    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, PacklySurfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent.background),
            )
            Column(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(PacklySpacing.md),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
                ) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${list.entries.size} Items",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (categoryChips.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                            verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                        ) {
                            categoryChips.forEach { label ->
                                ListCategoryChip(label = label, accent = accent)
                            }
                        }
                    }
                    if (list.description.isNotBlank()) {
                        Text(
                            text = list.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onRename,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.width(PacklySpacing.xs))
                        Text("Rename")
                    }
                    TextButton(onClick = onDuplicate) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(PacklySpacing.xs))
                        Text("Duplicate")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null)
                        Spacer(Modifier.width(PacklySpacing.xs))
                        Text("Archive")
                    }
                }
            }
        }
    }
}

@Composable
private fun ListCategoryChip(label: String, accent: ListAccent) {
    Surface(
        shape = RoundedCornerShape(PacklyRadius.default),
        color = accent.background,
        contentColor = accent.content,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberListAccent(listName: String): ListAccent = when {
    listName.contains("business", ignoreCase = true) -> ListAccent(PacklyPrimaryFixed, PacklyOnPrimaryFixedVariant)
    listName.contains("ski", ignoreCase = true) -> ListAccent(PacklySecondaryFixed, PacklyOnSecondaryFixedVariant)
    else -> ListAccent(PacklyTertiaryFixed, PacklyOnTertiaryFixedVariant)
}

@Immutable
private data class ListAccent(val background: Color, val content: Color)
