package com.packly.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.ListEntry
import com.packly.app.data.repository.PacklyRepository
import com.packly.app.ui.components.CategoryBadge
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistCreateScreen(
    listName: String,
    repository: PacklyRepository,
    onBack: () -> Unit,
    onListCreated: (ItemList) -> Unit
) {
    val items by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var checkedIds by remember { mutableStateOf<Set<String>>(setOf()) }
    val scope = rememberCoroutineScope()

    val sortedCategories = remember(categories) { categories.sortedBy { it.sortOrder } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.name.contains(searchQuery, ignoreCase = true) ||
            categoryMap[it.categoryId]?.name?.contains(searchQuery, ignoreCase = true) == true }
    }

    val itemsByCategory = remember(filteredItems, sortedCategories) {
        filteredItems.groupBy { it.categoryId }
            .mapValues { (_, v) -> v.sortedBy { it.name } }
            .toSortedMap(kotlin.comparisons.compareBy { key -> sortedCategories.indexOfFirst { it.id == key } })
    }

    val selectedCount = checkedIds.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist: $listName") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                        text = "No items found. Add items first in Edit Items.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsByCategory.forEach { (categoryId, catItems) ->
                        val cat = categoryMap[categoryId]
                        if (cat != null) {
                            item(key = "header_" + categoryId) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryBadge(category = cat)
                                }
                            }
                        }
                        items(catItems, key = { it.id }) { item ->
                            ChecklistItemRow(
                                item = item,
                                isChecked = checkedIds.contains(item.id),
                                onToggle = {
                                    checkedIds = if (checkedIds.contains(item.id))
                                        checkedIds - item.id
                                    else
                                        checkedIds + item.id
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCount == 1) "1 item selected" else "$selectedCount items selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = {
                        val entries = checkedIds.map { itemId ->
                            ListEntry(itemId = itemId, quantity = 1)
                        }
                        val newList = ItemList(
                            id = UUID.randomUUID().toString(),
                            name = listName,
                            items = entries,
                            createdAt = System.currentTimeMillis()
                        )
                        onListCreated(newList)
                    },
                    enabled = selectedCount > 0
                ) {
                    Text("Create List ($selectedCount)")
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: Item,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isChecked) "Checked" else "Unchecked",
                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
