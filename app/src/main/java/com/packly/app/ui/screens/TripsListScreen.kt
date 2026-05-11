package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Trip
import com.packly.app.data.model.TripEntry
import com.packly.app.data.repository.PacklyRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListScreen(
    repository: PacklyRepository,
    onTripClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val trips by repository.getTrips().collectAsState(initial = emptyList())
    val lists by repository.getLists().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var tripName by remember { mutableStateOf("") }
    var selectedListId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trips") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
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
                text = { Text("New Trip") }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No trips yet. Create one from a list!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    ElevatedCard(onClick = { onTripClick(trip.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FlightTakeoff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(trip.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(trip.date.ifEmpty { "No date set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${trip.items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { scope.launch { repository.deleteTrip(trip.id) } }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Trip") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tripName,
                        onValueChange = { tripName = it },
                        label = { Text("Trip name") },
                        placeholder = { Text("e.g. Summer 2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Materialize from list (optional):", style = MaterialTheme.typography.labelMedium)
                    lists.forEach { list ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedListId == list.id, onClick = { selectedListId = list.id })
                            Text("${list.name} (${list.items.size} items)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val tripItems = if (selectedListId.isNotBlank()) {
                                val src = lists.find { it.id == selectedListId }
                                src?.items?.map { TripEntry(itemId = it.itemId, quantity = it.quantity) } ?: emptyList()
                            } else emptyList()
                            repository.createTrip(Trip(
                                id = UUID.randomUUID().toString(),
                                name = tripName,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                listId = selectedListId.ifBlank { null },
                                items = tripItems,
                                createdAt = System.currentTimeMillis()
                            ))
                        }
                        showCreateDialog = false
                    },
                    enabled = tripName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
