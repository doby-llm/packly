@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*

@Composable
fun ListDetailScreen(doc: PacklyAppDocument, listId: ListId, onBack: () -> Unit, onToggle: (ItemId) -> Unit) {
    val list = doc.lists.firstOrNull { it.id == listId }
    val selected = list?.entries?.mapNotNull { it.itemId }?.toSet() ?: emptySet()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(list?.name ?: "List") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
    ) { padding ->
        if (list == null) Text("List not found", Modifier.padding(padding).padding(20.dp)) else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
            item { Text("Select items from your library. Changes snapshot names and quantities into this template.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(doc.categories.firstOrNull { it.id == item.categoryId }?.label ?: "Unknown") },
                    leadingContent = { Checkbox(item.id in selected, onCheckedChange = { onToggle(item.id) }) },
                )
            }
        }
    }
}
