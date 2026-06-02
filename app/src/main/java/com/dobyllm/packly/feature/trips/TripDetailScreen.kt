@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.ui.component.PacklyProgress

@Composable
fun TripDetailScreen(doc: PacklyAppDocument, tripId: TripId, onBack: () -> Unit, onPack: () -> Unit, onReset: () -> Unit) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var showResetConfirm by remember(tripId) { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(trip?.name ?: "Trip") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
    ) { padding ->
        if (trip == null) Text("Trip not found", Modifier.padding(padding).padding(20.dp)) else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(trip.destination.ifBlank { "No destination" }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { PacklyProgress(trip.entries.count { it.isPacked }, trip.entries.size) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onPack) { Text("Start packing") }; OutlinedButton(onClick = { showResetConfirm = true }) { Text("Reset packed items") } } }
            items(trip.entries, key = { it.id }) { entry -> ListItem(headlineContent = { Text(entry.nameSnapshot) }, supportingContent = { Text("Qty ${entry.quantity} • ${doc.categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.label ?: "Unknown"}") }) }
        }
    }
    if (trip != null && showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset packed items?") },
            text = { Text("All packed checkmarks for ${trip.name} will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
        )
    }
}
