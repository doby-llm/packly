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
    val selectedItems = remember { mutableStateListOf<ItemId>() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start trip", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Trip name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
            Text("Use a list", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sourceListId == null, onClick = { sourceListId = null }, label = { Text("Blank") })
                doc.lists.filterNot { it.isArchived }.forEach { list -> FilterChip(selected = sourceListId == list.id, onClick = { sourceListId = list.id }, label = { Text(list.name) }) }
            }
            Text("Add isolated items", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                doc.items.filterNot { it.isArchived }.take(12).forEach { item -> FilterChip(selected = item.id in selectedItems, onClick = { if (item.id in selectedItems) selectedItems.remove(item.id) else selectedItems.add(item.id) }, label = { Text(item.name) }) }
            }
            Button(enabled = name.trim().isNotEmpty(), onClick = { onCreate(name, destination, sourceListId, selectedItems.toSet()); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Save trip") }
        }
    }
}
