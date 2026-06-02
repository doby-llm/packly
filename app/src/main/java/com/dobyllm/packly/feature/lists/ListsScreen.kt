@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

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
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ListCard

@Composable
fun ListsScreen(doc: PacklyAppDocument, onBack: () -> Unit, onCreate: (String, String, Set<ItemId>) -> Unit, onOpen: (ListId) -> Unit, onUseForTrip: (ListId) -> Unit, onDelete: (ListId) -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Lists") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Rounded.Add, contentDescription = "Create list") } },
    ) { padding ->
        val lists = doc.lists.filterNot { it.isArchived }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (lists.isEmpty()) item { EmptyState("No packing lists yet.", "Create a reusable list for weekends, work trips, or holidays.", "Create list") { showCreate = true } }
            items(lists, key = { it.id }) { list -> ListCard(list, onOpen = { onOpen(list.id) }, onUseForTrip = { onUseForTrip(list.id) }, onDelete = { onDelete(list.id) }) }
        }
    }
    if (showCreate) CreateListSheet(doc, onDismiss = { showCreate = false }, onCreate = onCreate)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CreateListSheet(doc: PacklyAppDocument, onDismiss: () -> Unit, onCreate: (String, String, Set<ItemId>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ItemId>() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create list", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("List name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Text("Checklist items", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                doc.items.filterNot { it.isArchived }.take(12).forEach { item -> FilterChip(selected = item.id in selected, onClick = { if (item.id in selected) selected.remove(item.id) else selected.add(item.id) }, label = { Text(item.name) }) }
            }
            Button(enabled = name.trim().isNotEmpty(), onClick = { onCreate(name, description, selected.toSet()); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Save list") }
        }
    }
}
