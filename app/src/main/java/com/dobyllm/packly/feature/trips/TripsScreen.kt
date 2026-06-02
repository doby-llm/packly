@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

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
import com.dobyllm.packly.ui.component.TripCard

@Composable
fun TripsScreen(doc: PacklyAppDocument, onBack: () -> Unit, onCreate: (String, String, ListId?, Set<ItemId>) -> Unit, onOpen: (TripId) -> Unit, onPack: (TripId) -> Unit, onDelete: (TripId) -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<PacklyTrip?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Trips") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Rounded.Add, contentDescription = "Start trip") } },
    ) { padding ->
        val trips = doc.trips.filter { it.status != TripStatus.Archived }.sortedByDescending { it.updatedAt }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (trips.isEmpty()) item { EmptyState("No trips planned.", "Turn a packing list into a trip checklist when you know where you’re going.", "Start trip") { showCreate = true } }
            items(trips, key = { it.id }) { trip -> TripCard(trip, onOpen = { onOpen(trip.id) }, onPack = { onPack(trip.id) }, onDelete = { tripToDelete = trip }) }
        }
    }
    if (showCreate) CreateTripSheet(doc, onDismiss = { showCreate = false }, onCreate = onCreate)
    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Archive ${trip.name}?") },
            text = { Text("The trip will be hidden from active trips. Its packed state can’t be changed after archiving.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(trip.id)
                        tripToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { tripToDelete = null }) { Text("Cancel") } },
        )
    }
}
