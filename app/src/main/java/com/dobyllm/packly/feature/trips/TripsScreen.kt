@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.model.TripStatus
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.TripCard
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun TripsScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onFabActionChange: ((PacklyFabAction?) -> Unit)? = null,
    onCreate: (String, String, ListId?, Set<ItemId>, Map<ItemId, Int>, InstantString?) -> Unit,
    onOpen: (TripId) -> Unit,
    onPack: (TripId) -> Unit,
    onDelete: (TripId) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<PacklyTrip?>(null) }
    val trips = doc.trips.filter { it.status != TripStatus.Archived }.sortedByDescending { it.updatedAt }

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Add trip", onClick = { showCreate = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        if (trips.isEmpty()) {
            item { EmptyState("No trips planned.", "Build a trip checklist from a packing list or individual items.", "Start trip") { showCreate = true } }
        }
        items(trips, key = { it.id }) { trip ->
            TripCard(
                trip,
                onOpen = { onOpen(trip.id) },
                onPack = { onPack(trip.id) },
                onDelete = { tripToDelete = trip },
            )
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
