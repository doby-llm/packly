@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.dobyllm.packly.ui.component.CategoryHeader
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ItemRow
import com.dobyllm.packly.ui.component.PacklyFabAction
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
    val categories = doc.categories.sortedBy { it.sortOrder }
    val items = doc.items.filter { !it.isArchived && it.name.contains(query, ignoreCase = true) }

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Add item", onClick = { showAdd = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = PacklySpacing.marginMobile),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search items") },
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = PacklySpacing.sm),
        ) {
            categories.forEach { category ->
                val sectionItems = items.filter { it.categoryId == category.id }
                if (sectionItems.isNotEmpty()) {
                    item(key = "header_${category.key}") { CategoryHeader(category, "${sectionItems.size}") }
                    items(sectionItems, key = { it.id }) { item ->
                        ItemRow(
                            item,
                            category,
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item },
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                item { EmptyState("No matches", "Try another word or add a reusable item.", "Add item") { showAdd = true } }
            }
        }
    }

    if (showAdd) {
        EditItemSheet(
            categories = categories,
            existingNames = doc.items.filterNot { it.isArchived }.map { it.name },
            onDismiss = { showAdd = false },
            onSave = onAdd,
        )
    }
    itemToEdit?.let { item ->
        EditItemSheet(
            categories = categories,
            item = item,
            existingNames = doc.items.filter { !it.isArchived && it.id != item.id }.map { it.name },
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
