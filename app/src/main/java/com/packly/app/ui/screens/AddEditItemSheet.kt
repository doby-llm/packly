package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
fun AddEditItemSheet(
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
                title = { Text("Edit Backlog") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Edit, "Back") } },
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search items...") },
                singleLine = true
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                val cat = categoryMap[item.categoryId]
                                if (cat != null) {
                                    Spacer(Modifier.height(4.dp))
                                    CategoryBadge(category = cat)
                                }
                            }
                            IconButton(onClick = { scope.launch { repository.deleteItem(item.id) } }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
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
