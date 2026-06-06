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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.SelectableItemCard
import com.dobyllm.packly.ui.i18n.displayDescription
import com.dobyllm.packly.ui.i18n.displayLabel
import com.dobyllm.packly.ui.i18n.displayName
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ListDetailScreen(
    doc: PacklyAppDocument,
    listId: ListId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onToggle: (ItemId) -> Unit,
    onUseForTrip: (ListId) -> Unit,
) {
    val list = doc.lists.firstOrNull { it.id == listId }
    val selected = list?.entries?.mapNotNull { it.itemId }?.toSet() ?: emptySet()

    if (list == null) {
        EmptyState(
            title = stringResource(R.string.list_not_found_title),
            body = stringResource(R.string.list_not_found_body),
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
            ListDetailHeader(doc = doc, listId = listId, onUseForTrip = { onUseForTrip(listId) })
        }
        items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
            val category = doc.categories.firstOrNull { it.id == item.categoryId }
            SelectableItemCard(
                title = item.displayName(),
                subtitle = listOfNotNull(category?.displayLabel(), item.notes.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { stringResource(R.string.uncategorized) },
                selected = item.id in selected,
                onToggle = { onToggle(item.id) },
            )
        }
    }
}

@Composable
private fun ListDetailHeader(doc: PacklyAppDocument, listId: ListId, onUseForTrip: () -> Unit) {
    val list = doc.lists.first { it.id == listId }
    val categoryLabels = list.entries
        .sortedBy { it.sortOrder }
        .mapNotNull { entry -> doc.categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.displayLabel() }
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
                text = list.displayName(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = list.displayDescription().ifBlank { stringResource(R.string.list_detail_default_description) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.list_item_count, list.entries.size),
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
            Button(
                onClick = onUseForTrip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.action_use_for_trip))
            }
        }
    }
}
