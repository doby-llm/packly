@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument

@Composable
fun AddItemsToListSheet(doc: PacklyAppDocument, selectedIds: Set<String>, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Select items", style = MaterialTheme.typography.titleLarge) }
            items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(doc.categories.firstOrNull { it.id == item.categoryId }?.label ?: "Unknown") },
                    leadingContent = { Checkbox(item.id in selectedIds, onCheckedChange = { onToggle(item.id) }) },
                )
            }
        }
    }
}
