package com.dobyllm.packly.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.model.TripStatus
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.feature.trips.CreateTripSheet
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.TripSummaryCard
import com.dobyllm.packly.ui.token.PacklySpacing
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onCreateTrip: (String, String, List<ListId>, Set<ItemId>, Map<ItemId, Int>, InstantString?) -> Unit,
    onOpenTrip: (TripId) -> Unit,
) {
    var showCreateTrip by remember { mutableStateOf(false) }
    val activeTrips = doc.trips
        .filter { it.status != TripStatus.Archived }
        .sortedByDescending { it.updatedAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.md),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        item { HomeHero(activeTripCount = activeTrips.size) }
        item { SectionTitle("Active Trips") }
        if (activeTrips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "No trips yet",
                        body = "Create your first trip and Packly will keep you ready.",
                        actionLabel = "Create trip",
                        onAction = { showCreateTrip = true },
                    )
                }
            }
        } else {
            items(activeTrips, key = { it.id }) { trip ->
                val packed = trip.entries.count { it.isPacked }
                val total = trip.entries.size.coerceAtLeast(1)
                val progress = packed / total.toFloat()
                val packBy = PacklyDeadlineFormatter.formatDisplay(trip.packBy)
                val metadata = listOfNotNull(
                    trip.startDate?.let { start -> trip.endDate?.let { end -> "$start - $end" } ?: start },
                    trip.destination.takeIf { it.isNotBlank() },
                    "$packed/${trip.entries.size} packed",
                    packBy?.let { "Pack by $it" },
                ).joinToString(" • ")
                val chips = listOfNotNull(
                    trip.destination.takeIf { it.isNotBlank() },
                    packBy?.let { "Pack by $it" },
                    trip.entries.size.takeIf { it > 0 }?.let { "$it items" },
                )

                TripSummaryCard(
                    title = trip.name,
                    metadata = metadata,
                    percentLabel = "${(progress * 100).roundToInt()}% Packed",
                    progress = progress,
                    chips = chips,
                    accentColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { onOpenTrip(trip.id) },
                )
            }
        }
    }

    if (showCreateTrip) {
        CreateTripSheet(
            doc = doc,
            onDismiss = { showCreateTrip = false },
            onCreate = onCreateTrip,
        )
    }
}

@Composable
private fun HomeHero(activeTripCount: Int) {
    val greeting = remember { greetingFor(LocalTime.now()) }

    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "You have $activeTripCount upcoming ${if (activeTripCount == 1) "trip" else "trips"}. Let's get you prepared and ready to go.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun greetingFor(localTime: LocalTime): String = when (localTime.hour) {
    in 5..11 -> "Good Morning."
    in 12..16 -> "Good Afternoon."
    in 17..21 -> "Good Evening."
    else -> "Good Night."
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
