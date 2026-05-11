package com.packly.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.packly.app.model.Category
import com.packly.app.model.PackingItem
import com.packly.app.model.Trip
import com.packly.app.ui.components.CategoryBadge
import com.packly.app.ui.components.CategoryDot

// ──────────────────────────────────────────────────────────────────────────
// Screen 2: Trip Detail — packing list for one trip.
// Items grouped by category, check/uncheck, FAB to add item.
// ──────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    onBack: () -> Unit,
    onToggleItem: (PackingItem) -> Unit,
    onAddItem: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    val packedCount = trip.items.count { it.isPacked }
    val totalCount = trip.items.size
    val progressText = if (totalCount > 0) "$packedCount / $totalCount packed" else "No items yet"

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit trip") },
                            onClick = {
                                showMenu = false
                                onEditTrip()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete trip") },
                            onClick = {
                                showMenu = false
                                onDeleteTrip()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Trip header
            item {
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Items grouped by category
            val groupedItems = trip.items.groupBy { it.category }
            val categoriesInOrder = Category.entries.filter { it in groupedItems }

            if (trip.items.isEmpty()) {
                item {
                    Text(
                        text = "Tap + to add your first item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            for (category in categoriesInOrder) {
                val items = groupedItems[category] ?: continue

                // Category header
                item(key = "header_${category.name}") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CategoryDot(category)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${items.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                // Items in this category
                items(items, key = { it.id }) { item ->
                    PackingItemRow(
                        item = item,
                        onToggle = { onToggleItem(item) },
                    )
                }
            }

            // Bottom spacer for FAB clearance
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PackingItemRow(
    item: PackingItem,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPacked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPacked) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Check / unchecked circle
            Icon(
                imageVector = if (item.isPacked)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.RadioButtonUnchecked,
                contentDescription = if (item.isPacked) "Mark unpacked" else "Mark packed",
                tint = if (item.isPacked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isPacked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isPacked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                if (item.quantity > 1) {
                    Text(
                        text = "x${item.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            CategoryBadge(category = item.category)
        }
    }
}
