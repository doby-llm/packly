@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.SelectableItemCard
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ListDetailScreen(
    doc: PacklyAppDocument,
    listId: ListId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onToggle: (ItemId) -> Unit,
) {
    val list = doc.lists.firstOrNull { it.id == listId }
    val selected = list?.entries?.mapNotNull { it.itemId }?.toSet() ?: emptySet()

    if (list == null) {
        EmptyState(
            title = "This list could not be found",
            body = "Go back to Lists and choose another template.",
            modifier = Modifier.padding(contentPadding).padding(PacklySpacing.md),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        item {
            ListDetailHeader(doc = doc, listId = listId)
        }
        items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
            val category = doc.categories.firstOrNull { it.id == item.categoryId }
            SelectableItemCard(
                title = item.name,
                subtitle = listOfNotNull(category?.label, item.notes.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Uncategorized" },
                selected = item.id in selected,
                onToggle = { onToggle(item.id) },
            )
        }
    }
}

@Composable
private fun ListDetailHeader(doc: PacklyAppDocument, listId: ListId) {
    val list = doc.lists.first { it.id == listId }
    val categoryLabels = list.entries
        .sortedBy { it.sortOrder }
        .mapNotNull { entry -> doc.categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.label }
        .distinct()
        .take(4)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Text(
                text = list.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = list.description.ifBlank { "Select items from your library. Changes snapshot names and quantities into this template." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${list.entries.size} Items",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (categoryLabels.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                ) {
                    categoryLabels.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(PacklyRadius.default),
                            color = MaterialTheme.colorScheme.secondaryFixed,
                            contentColor = MaterialTheme.colorScheme.onSecondaryFixedVariant,
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
