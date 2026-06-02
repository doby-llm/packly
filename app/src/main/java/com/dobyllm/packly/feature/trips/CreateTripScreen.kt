@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CreateTripSheet(
    doc: PacklyAppDocument,
    onDismiss: () -> Unit,
    onCreate: (String, String, ListId?, Set<ItemId>, Map<ItemId, Int>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var sourceListId by remember { mutableStateOf<ListId?>(null) }
    var itemQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<ItemId>() }
    val quantities = remember { mutableStateMapOf<ItemId, Int>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.trips.any { it.status != TripStatus.Archived && it.name.equals(name.trim(), ignoreCase = true) }
    val sourceEntries = sourceListId
        ?.let { id -> doc.lists.firstOrNull { it.id == id }?.entries }
        ?: emptyList()
    val sourceItemIds = sourceEntries.mapNotNull { it.itemId }.toSet()
    val selectedItemIds = remember(sourceItemIds, selectedItems.toList()) { sourceItemIds + selectedItems }
    val duplicateSourceCount = selectedItems.count { it in sourceItemIds }
    val reviewItems = remember(selectedItemIds, doc.items, sourceEntries) {
        buildTripReviewItems(selectedItemIds, doc.items, sourceEntries)
    }

    LaunchedEffect(selectedItemIds) {
        selectedItemIds.forEach { itemId -> quantities.putIfAbsent(itemId, 1) }
        quantities.keys.toList().filterNot { it in selectedItemIds }.forEach { quantities.remove(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start trip", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Trip name") },
                supportingText = { if (duplicateName) Text("An active trip with this name already exists.") },
                isError = duplicateName,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showDestination) {
                OutlinedTextField(
                    destination,
                    { destination = it },
                    label = { Text("Destination (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { showDestination = true }) { Text("Add destination (optional)") }
            }
            Text("Use a list", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sourceListId == null, onClick = { sourceListId = null }, label = { Text("Blank") })
                doc.lists.filterNot { it.isArchived }.forEach { list -> FilterChip(selected = sourceListId == list.id, onClick = { sourceListId = list.id }, label = { Text(list.name) }) }
            }
            Text("Add isolated items", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(itemQuery, { itemQuery = it }, label = { Text("Search all ${activeItems.size} items") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                matchingItems.forEach { item ->
                    FilterChip(
                        selected = item.id in selectedItems,
                        onClick = { if (item.id in selectedItems) selectedItems.remove(item.id) else selectedItems.add(item.id) },
                        label = { Text(item.name) },
                    )
                }
            }
            if (matchingItems.isEmpty()) Text("No items match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (duplicateSourceCount > 0) {
                Text(
                    "$duplicateSourceCount selected item(s) already exist in the source list and will be included only once.",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (reviewItems.isNotEmpty()) {
                Text("Review quantities", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reviewItems.forEach { reviewItem ->
                        val category = doc.categories.firstOrNull { it.id == reviewItem.categoryId }?.label ?: "Unknown"
                        QuantityReviewRow(
                            name = reviewItem.name,
                            category = category,
                            quantity = quantities[reviewItem.itemId] ?: 1,
                            onQuantityChange = { quantity -> quantities[reviewItem.itemId] = quantity.coerceAtLeast(1) },
                        )
                    }
                }
            }
            Button(
                enabled = name.trim().isNotEmpty() && !duplicateName,
                onClick = {
                    onCreate(name, destination, sourceListId, selectedItems.toSet(), quantities.toMap())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save trip") }
        }
    }
}

private data class TripReviewItem(
    val itemId: ItemId,
    val name: String,
    val categoryId: CategoryId,
    val sortOrder: Int,
)

private fun buildTripReviewItems(
    selectedItemIds: Set<ItemId>,
    items: List<PacklyItem>,
    sourceEntries: List<PacklyListEntry>,
): List<TripReviewItem> {
    val sourceReviewItems = sourceEntries.mapNotNull { entry ->
        val itemId = entry.itemId ?: return@mapNotNull null
        if (itemId !in selectedItemIds) return@mapNotNull null
        TripReviewItem(itemId, entry.itemNameSnapshot, entry.categoryIdSnapshot, entry.sortOrder)
    }
    val sourceIds = sourceReviewItems.map { it.itemId }.toSet()
    val isolatedReviewItems = items
        .filter { item -> item.id in selectedItemIds && item.id !in sourceIds }
        .mapIndexed { index, item -> TripReviewItem(item.id, item.name, item.categoryId, sourceEntries.size + index) }
    return (sourceReviewItems + isolatedReviewItems).distinctBy { it.itemId }.sortedBy { it.sortOrder }
}

@Composable
private fun QuantityReviewRow(name: String, category: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease $name quantity")
                }
                Text("×$quantity", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase $name quantity")
                }
            }
        }
    }
}
