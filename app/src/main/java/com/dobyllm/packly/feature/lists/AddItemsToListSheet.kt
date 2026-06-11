@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import androidx.compose.ui.text.font.FontWeight
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.ui.component.SelectableItemCard
import com.dobyllm.packly.ui.i18n.displayLabel
import com.dobyllm.packly.ui.i18n.displayName
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun AddItemsToListSheet(doc: PacklyAppDocument, selectedIds: Set<String>, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    val activeItems = doc.items.filterNot { it.isArchived }
    val activeCategories = doc.categories
        .filterNot { it.isArchived }
        .filter { category -> activeItems.any { it.categoryId == category.id } }
        .sortedBy { it.sortOrder }
    val categoryLabelById = doc.categories.associate { it.id to it.displayLabel() }
    val itemNameById = activeItems.associate { it.id to it.displayName() }
    val filteredItems = activeItems
        .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
        .filter { item ->
            val categoryLabel = categoryLabelById[item.categoryId].orEmpty()
            query.isBlank() ||
                (itemNameById[item.id] ?: item.name).contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true) ||
                categoryLabel.contains(query, ignoreCase = true)
        }
    val sections = buildItemSections(
        items = filteredItems,
        doc = doc,
        fallbackLabel = stringResource(R.string.uncategorized),
        categoryLabelById = categoryLabelById,
        itemNameById = itemNameById,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            item {
                Text(
                    stringResource(R.string.select_items_title),
                    modifier = Modifier.padding(bottom = PacklySpacing.xs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Text(
                    pluralStringResource(R.plurals.selected_grouped_by_category, selectedIds.size, selectedIds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                PacklySelectionSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = pluralStringResource(R.plurals.search_items_count_label, activeItems.size, activeItems.size),
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    PacklyCategoryFilterChip(
                        label = stringResource(R.string.filter_all_label),
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                    )
                    activeCategories.forEach { category ->
                        PacklyCategoryFilterChip(
                            label = category.displayLabel(),
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == category.id) null else category.id },
                        )
                    }
                }
            }
            if (sections.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_items_match_filter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                sections.forEach { section ->
                    item(key = "header-${section.categoryId}") {
                        Text(
                            text = stringResource(R.string.category_count_label, section.label, section.items.size),
                            modifier = Modifier.padding(top = PacklySpacing.base),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(section.items, key = { it.id }) { item ->
                        SelectableItemCard(
                            title = itemNameById[item.id] ?: item.name,
                            subtitle = item.notes.takeIf { it.isNotBlank() } ?: section.label,
                            selected = item.id in selectedIds,
                            onToggle = { onToggle(item.id) },
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PacklySelectionSearchField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(PacklyRadius.default),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun PacklyCategoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(PacklyRadius.default),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            selectedContainerColor = MaterialTheme.colorScheme.primaryFixed,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
        ),
    )
}

private fun buildItemSections(
    items: List<PacklyItem>,
    doc: PacklyAppDocument,
    fallbackLabel: String,
    categoryLabelById: Map<CategoryId, String>,
    itemNameById: Map<ItemId, String>,
): List<ItemSection> {
    val categories = doc.categories.associateBy { it.id }
    return items
        .groupBy { it.categoryId }
        .map { (categoryId, categoryItems) ->
            val category = categories[categoryId]
            ItemSection(
                categoryId = categoryId,
                label = categoryLabelById[categoryId] ?: fallbackLabel,
                sortOrder = category?.sortOrder ?: Int.MAX_VALUE,
                items = categoryItems.sortedBy { (itemNameById[it.id] ?: it.name).lowercase() },
            )
        }
        .sortedWith(compareBy<ItemSection> { it.sortOrder }.thenBy { it.label })
}

private data class ItemSection(
    val categoryId: CategoryId,
    val label: String,
    val sortOrder: Int,
    val items: List<PacklyItem>,
)
