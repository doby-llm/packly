@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ListCard
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ListsScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onFabActionChange: ((PacklyFabAction?) -> Unit)? = null,
    onCreate: (String, String, Set<ItemId>) -> Unit,
    onOpen: (ListId) -> Unit,
    onUseForTrip: (ListId) -> Unit,
    onDelete: (ListId) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<PacklyList?>(null) }
    val lists = doc.lists.filterNot { it.isArchived }

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Add list", onClick = { showCreate = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        if (lists.isEmpty()) {
            item { EmptyState("No packing lists yet.", "Create a reusable list for weekends, work trips, or holidays.", "Create list") { showCreate = true } }
        }
        items(lists, key = { it.id }) { list ->
            ListCard(
                list,
                onOpen = { onOpen(list.id) },
                onUseForTrip = { onUseForTrip(list.id) },
                onDelete = { listToDelete = list },
            )
        }
    }

    if (showCreate) CreateListSheet(doc, onDismiss = { showCreate = false }, onCreate = onCreate)
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Archive ${list.name}?") },
            text = { Text("The list will be hidden, but trips already created from it keep their snapshots.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(list.id)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { listToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CreateListSheet(doc: PacklyAppDocument, onDismiss: () -> Unit, onCreate: (String, String, Set<ItemId>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var itemQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ItemId>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.lists.any { !it.isArchived && it.name.equals(name.trim(), ignoreCase = true) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding().imePadding()) {
            Text(
                "Create list",
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("List name") },
                    supportingText = { if (duplicateName) Text("An active list with this name already exists.") },
                    isError = duplicateName,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Text("Checklist items", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(itemQuery, { itemQuery = it }, label = { Text("Search all ${activeItems.size} items") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchingItems.forEach { item ->
                        FilterChip(
                            selected = item.id in selected,
                            onClick = { if (item.id in selected) selected.remove(item.id) else selected.add(item.id) },
                            label = { Text(item.name) },
                        )
                    }
                }
                if (matchingItems.isEmpty()) Text("No items match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                enabled = name.trim().isNotEmpty() && !duplicateName,
                onClick = {
                    onCreate(name, description, selected.toSet())
                    onDismiss()
                },
                modifier = Modifier.padding(20.dp).fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) { Text("Save list") }
        }
    }
}
