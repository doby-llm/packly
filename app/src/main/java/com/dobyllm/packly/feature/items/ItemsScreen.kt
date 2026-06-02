@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.ui.component.CategoryHeader
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ItemRow

@Composable
fun ItemsScreen(doc: PacklyAppDocument, onBack: () -> Unit, onAdd: (String, CategoryId, Int, String) -> Unit, onDelete: (ItemId) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val categories = doc.categories.sortedBy { it.sortOrder }
    val items = doc.items.filter { !it.isArchived && it.name.contains(query, ignoreCase = true) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Items") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, contentDescription = "Add item") } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Search items") }, modifier = Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                categories.forEach { category ->
                    val sectionItems = items.filter { it.categoryId == category.id }
                    if (sectionItems.isNotEmpty()) {
                        item(key = "header_${category.key}") { CategoryHeader(category, "${sectionItems.size}") }
                        items(sectionItems, key = { it.id }) { item -> ItemRow(item, category, onEdit = {}, onDelete = { onDelete(item.id) }) }
                    }
                }
                if (items.isEmpty()) item { EmptyState("No matches", "Try another word or add a reusable item.", "Add item") { showAdd = true } }
            }
        }
    }
    if (showAdd) EditItemSheet(categories, onDismiss = { showAdd = false }, onSave = onAdd)
}
