@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CreateTripSheet(doc: PacklyAppDocument, onDismiss: () -> Unit, onCreate: (String, String, ListId?, Set<ItemId>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var sourceListId by remember { mutableStateOf<ListId?>(null) }
    var itemQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<ItemId>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.trips.any { it.status != TripStatus.Archived && it.name.equals(name.trim(), ignoreCase = true) }
    val sourceItemIds = sourceListId
        ?.let { id -> doc.lists.firstOrNull { it.id == id }?.entries?.mapNotNull { it.itemId }?.toSet() }
        ?: emptySet()
    val duplicateSourceCount = selectedItems.count { it in sourceItemIds }
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
            OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
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
            Button(
                enabled = name.trim().isNotEmpty() && !duplicateName,
                onClick = {
                    onCreate(name, destination, sourceListId, selectedItems.toSet())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save trip") }
        }
    }
}
