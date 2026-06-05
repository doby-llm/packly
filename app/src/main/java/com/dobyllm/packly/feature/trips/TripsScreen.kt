@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
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
    onCreate: (String, String, List<ListId>, Set<ItemId>, Map<ItemId, Int>, InstantString?) -> Unit,
    onOpen: (TripId) -> Unit,
    onPack: (TripId) -> Unit,
    onDelete: (TripId) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<PacklyTrip?>(null) }
    val visibleTrips = doc.trips.filter { it.status != TripStatus.Archived }
    val activeTrips = visibleTrips
        .filter { it.status != TripStatus.Completed }
        .sortedByDescending { it.updatedAt }
    val completedTrips = visibleTrips
        .filter { it.status == TripStatus.Completed }
        .sortedByDescending { it.updatedAt }

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Create trip", onClick = { showCreate = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        item { TripsHeader(activeCount = activeTrips.size, completedCount = completedTrips.size) }
        if (visibleTrips.isEmpty()) {
            item {
                EmptyState(
                    title = "No trips planned",
                    body = "Build a trip checklist from a packing list or individual items.",
                    actionLabel = "Create trip",
                    onAction = { showCreate = true },
                )
            }
        } else {
            if (activeTrips.isNotEmpty()) {
                item { TripsSectionTitle("Active trips") }
                items(activeTrips, key = { it.id }) { trip ->
                    TripCard(
                        trip,
                        onOpen = { onOpen(trip.id) },
                        onPack = { onPack(trip.id) },
                        onDelete = { tripToDelete = trip },
                    )
                }
            }
            if (completedTrips.isNotEmpty()) {
                item { TripsSectionTitle("Completed trips") }
                items(completedTrips, key = { it.id }) { trip ->
                    TripCard(
                        trip,
                        onOpen = { onOpen(trip.id) },
                        onPack = { onPack(trip.id) },
                        onDelete = { tripToDelete = trip },
                    )
                }
            }
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

@Composable
private fun TripsHeader(activeCount: Int, completedCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PacklySpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
    ) {
        Text(
            text = "Trips",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val completedCopy = completedCount.takeIf { it > 0 }?.let { " Completed trips stay lower priority." } ?: ""
        Text(
            text = "Prepare ${activeCount.coerceAtLeast(0)} active ${if (activeCount == 1) "trip" else "trips"}.$completedCopy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
