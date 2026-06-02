@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.ui.component.CategoryRowsContainer
import com.dobyllm.packly.ui.component.CategorySectionCard
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ItemRow
import com.dobyllm.packly.ui.component.ItemRowDivider
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.PacklySearchFilterRow
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
    var showAdd by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PacklyItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PacklyItem?>(null) }
    val categories = doc.categories.filterNot { it.isArchived }.sortedBy { it.sortOrder }
    val activeItems = doc.items.filterNot { it.isArchived }
    val duplicateNames = remember(activeItems) {
        activeItems
            .groupBy { it.name.trim().lowercase() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
    }
    val filteredItems = activeItems.filter { item ->
        query.isBlank() || item.name.contains(query, ignoreCase = true) || item.notes.contains(query, ignoreCase = true)
    }

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
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.lg),
    ) {
        item {
            PacklySearchFilterRow(
                query = query,
                onQueryChange = { query = it },
                // Category/status filters are not implemented yet; keep the affordance visibly non-interactive.
                onFilterClick = null,
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
                        body = "Try another word or add a reusable item.",
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

private fun List<PacklyItem>.itemCountLabel(): String {
    val count = size
    return "$count ${if (count == 1) "Item" else "Items"}"
}
