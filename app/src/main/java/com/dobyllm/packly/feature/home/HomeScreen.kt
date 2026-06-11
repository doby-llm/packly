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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.model.TripStatus
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.TripSummaryCard
import com.dobyllm.packly.ui.token.PacklySpacing
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onStartCreateTrip: () -> Unit,
    onOpenTrip: (TripId) -> Unit,
) {
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
        item { SectionTitle(stringResource(R.string.home_active_trips_title)) }
        if (activeTrips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(R.string.home_empty_trips_title),
                        body = stringResource(R.string.home_empty_trips_body),
                        actionLabel = stringResource(R.string.action_create_trip),
                        onAction = onStartCreateTrip,
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
                    pluralStringResource(R.plurals.packed_fraction_short, trip.entries.size, packed, trip.entries.size),
                    packBy?.let { stringResource(R.string.pack_by_value, it) },
                ).joinToString(" • ")
                val chips = listOfNotNull(
                    trip.destination.takeIf { it.isNotBlank() },
                    packBy?.let { stringResource(R.string.pack_by_value, it) },
                    trip.entries.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.item_count_lower, it, it) },
                )

                TripSummaryCard(
                    title = trip.name,
                    metadata = metadata,
                    percentLabel = stringResource(R.string.percent_packed_label, (progress * 100).roundToInt()),
                    progress = progress,
                    chips = chips,
                    accentColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { onOpenTrip(trip.id) },
                )
            }
        }
    }

}

@Composable
private fun HomeHero(activeTripCount: Int) {
    val greeting = greetingFor(LocalTime.now())

    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = pluralStringResource(
                R.plurals.home_summary,
                activeTripCount,
                activeTripCount,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun greetingFor(localTime: LocalTime): String = when (localTime.hour) {
    in 5..11 -> stringResource(R.string.greeting_morning)
    in 12..16 -> stringResource(R.string.greeting_afternoon)
    in 17..21 -> stringResource(R.string.greeting_evening)
    else -> stringResource(R.string.greeting_night)
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
}
