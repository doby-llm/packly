package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Category
import com.packly.app.data.model.Item
import com.packly.app.data.repository.PacklyRepository
import com.packly.app.ui.components.CategoryBadge
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    repository: PacklyRepository,
    onBack: () -> Unit
) {
    val items by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Items") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Item") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true
            )
            if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No items match your search" else "Backlog is empty. Tap + to add!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemRow(item = item, category = categoryMap[item.categoryId],
                            onDelete = { scope.launch { repository.deleteItem(item.id) } })
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        AddItemDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, categoryId ->
                scope.launch {
                    repository.addItem(Item(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        categoryId = categoryId,
                        iconName = categories.find { it.id == categoryId }?.iconName ?: "more_horiz",
                        createdAt = System.currentTimeMillis()
                    ))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    category: Category?,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                if (category != null) {
                    Spacer(Modifier.height(4.dp))
                    CategoryBadge(category = category)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategory }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(itemName, selectedCategory) },
                enabled = itemName.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
