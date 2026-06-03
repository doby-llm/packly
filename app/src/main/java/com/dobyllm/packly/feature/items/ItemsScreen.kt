@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.ui.component.CategoryRowsContainer
import com.dobyllm.packly.ui.component.CategorySectionCard
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ItemRow
import com.dobyllm.packly.ui.component.ItemRowDivider
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.PacklySearchFilterRow
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ItemsScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onFabActionChange: ((PacklyFabAction?) -> Unit)? = null,
    onAdd: (String, CategoryId, String) -> Unit,
    onUpdate: (ItemId, String, CategoryId, String) -> Unit,
    onDelete: (ItemId) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PacklyItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PacklyItem?>(null) }
    var selectedCategoryIds by remember { mutableStateOf<Set<CategoryId>>(emptySet()) }
    val categories = doc.categories.filterNot { it.isArchived }.sortedBy { it.sortOrder }
    val activeItems = doc.items.filterNot { it.isArchived }
    val duplicateNames = remember(activeItems) {
        activeItems
            .groupBy { it.name.trim().lowercase() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
    }
    val filteredItems = filterLibraryItems(
        items = activeItems,
        query = query,
        selectedCategoryIds = selectedCategoryIds,
    )

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Add item", onClick = { showAdd = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            start = PacklySpacing.marginMobile,
            top = PacklySpacing.sm,
            end = PacklySpacing.marginMobile,
            bottom = PacklySpacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        item {
            PacklySearchFilterRow(
                query = query,
                onQueryChange = { query = it },
                onFilterClick = { showFilters = true },
            )
        }

        categories.forEach { category ->
            val sectionItems = filteredItems.filter { it.categoryId == category.id }
            if (sectionItems.isNotEmpty()) {
                item(key = "category_${category.key}") {
                    CategorySectionCard(
                        category = category,
                        countLabel = sectionItems.itemCountLabel(),
                    ) {
                        CategoryRowsContainer {
                            sectionItems.forEachIndexed { index, item ->
                                ItemRow(
                                    item = item,
                                    category = category,
                                    hasDuplicate = item.name.trim().lowercase() in duplicateNames,
                                    onEdit = { itemToEdit = item },
                                    onDelete = { itemToDelete = item },
                                )
                                if (index < sectionItems.lastIndex) ItemRowDivider()
                            }
                        }
                    }
                }
            }
        }

        val hasVisibleItems = filteredItems.any { item -> categories.any { it.id == item.categoryId } }
        if (!hasVisibleItems) {
            item {
                if (activeItems.isEmpty()) {
                    EmptyState(
                        title = "Your item library is empty",
                        body = "Add reusable essentials so future lists and trips start faster.",
                        actionLabel = "Add item",
                        onAction = { showAdd = true },
                    )
                } else {
                    EmptyState(
                        title = "No items found",
                        body = "Try a different search or category filter.",
                        actionLabel = "Add item",
                        onAction = { showAdd = true },
                    )
                }
            }
        }
    }

    if (showAdd) {
        EditItemSheet(
            categories = categories,
            existingNames = activeItems.map { it.name },
            onDismiss = { showAdd = false },
            onSave = onAdd,
        )
    }
    if (showFilters) {
        ItemsFilterSheet(
            categories = categories,
            allItems = activeItems,
            selectedCategoryIds = selectedCategoryIds,
            onCategorySelectionChange = { selectedCategoryIds = it },
            onDismiss = { showFilters = false },
        )
    }
    itemToEdit?.let { item ->
        EditItemSheet(
            categories = categories,
            item = item,
            existingNames = activeItems.filter { it.id != item.id }.map { it.name },
            onDismiss = { itemToEdit = null },
            onSave = { name, categoryId, notes -> onUpdate(item.id, name, categoryId, notes) },
        )
    }
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Archive ${item.name}?") },
            text = { Text("The item will be hidden from new lists and trips. Existing list and trip snapshots stay unchanged.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ItemsFilterSheet(
    categories: List<PacklyCategory>,
    allItems: List<PacklyItem>,
    selectedCategoryIds: Set<CategoryId>,
    onCategorySelectionChange: (Set<CategoryId>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = "Filter items",
                modifier = Modifier
                    .padding(horizontal = PacklySpacing.md)
                    .padding(top = PacklySpacing.base, bottom = PacklySpacing.sm),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PacklySpacing.md),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            ) {
                Text("Categories", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    ItemsFilterChip(
                        selected = selectedCategoryIds.isEmpty(),
                        onClick = { onCategorySelectionChange(emptySet()) },
                        label = "All categories (${allItems.countFor()})",
                    )
                    categories.forEach { category ->
                        val selected = category.id in selectedCategoryIds
                        ItemsFilterChip(
                            selected = selected,
                            onClick = {
                                val nextSelection = if (selected) {
                                    selectedCategoryIds - category.id
                                } else {
                                    selectedCategoryIds + category.id
                                }
                                onCategorySelectionChange(nextSelection)
                            },
                            label = "${category.label} (${allItems.countFor(category.id)})",
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PacklySpacing.md),
            ) { Text("Done") }
        }
    }
}

@Composable
private fun ItemsFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
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

internal fun filterLibraryItems(
    items: List<PacklyItem>,
    query: String,
    selectedCategoryIds: Set<CategoryId>,
): List<PacklyItem> = items.filter { item ->
    item.matchesQuery(query) &&
        item.matchesCategory(selectedCategoryIds) &&
        !item.isArchived
}

private fun PacklyItem.matchesQuery(query: String): Boolean =
    query.isBlank() || name.contains(query, ignoreCase = true) || notes.contains(query, ignoreCase = true)

private fun PacklyItem.matchesCategory(selectedCategoryIds: Set<CategoryId>): Boolean =
    selectedCategoryIds.isEmpty() || categoryId in selectedCategoryIds

private fun List<PacklyItem>.countFor(categoryId: CategoryId? = null): Int =
    count { item -> !item.isArchived && (categoryId == null || item.categoryId == categoryId) }

private fun List<PacklyItem>.itemCountLabel(): String {
    val count = size
    return "$count ${if (count == 1) "Item" else "Items"}"
}
