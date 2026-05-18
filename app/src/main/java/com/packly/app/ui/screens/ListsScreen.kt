package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.ItemList
import com.packly.app.data.repository.PacklyRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    repository: PacklyRepository,
    onBack: () -> Unit,
    onChecklistCreate: (String) -> Unit = {}
) {
    val lists by repository.getLists().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Lists") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New List") }
            )
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No lists yet. Tap + to create one!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(list.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("${list.items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { scope.launch { repository.deleteList(list.id) } }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var listName by remember { mutableStateOf("") }
        var selectedMode by remember { mutableStateOf("checklist") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New List") },
            text = {
                Column {
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        label = { Text("List name") },
                        placeholder = { Text("e.g. Beach Weekend") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Choose mode:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = selectedMode == "checklist",
                            onClick = { selectedMode = "checklist" },
                            label = { Text("Checklist") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = selectedMode == "tinder",
                            onClick = { selectedMode = "tinder" },
                            label = { Text("Tinder") },
                            leadingIcon = { Icon(Icons.Filled.TouchApp, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    if (listName.isNotBlank()) {
                        if (selectedMode == "checklist") {
                            onChecklistCreate(listName)
                        } else {
                            // Tinder mode — navigate to tinder screen
                        }
                    }
                }, enabled = listName.isNotBlank()) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}