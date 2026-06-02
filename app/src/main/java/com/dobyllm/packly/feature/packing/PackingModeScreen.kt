@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.packing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.TripEntryId
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.ui.component.CategoryHeader
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.PackingItemRow
import com.dobyllm.packly.ui.component.PacklyProgress
import com.dobyllm.packly.ui.token.PacklySpacing

private enum class PackingFilter { All, Unpacked, Packed }

@Composable
fun PackingModeScreen(
    doc: PacklyAppDocument,
    tripId: TripId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onToggle: (TripEntryId) -> Unit,
) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var filter by remember { mutableStateOf(PackingFilter.All) }

    if (trip == null) {
        Text("Trip not found", Modifier.padding(contentPadding).padding(20.dp))
    } else {
        val visible = trip.entries.filter {
            when (filter) {
                PackingFilter.All -> true
                PackingFilter.Unpacked -> !it.isPacked
                PackingFilter.Packed -> it.isPacked
            }
        }
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            contentPadding = PaddingValues(PacklySpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            item { Text(trip.name, style = MaterialTheme.typography.headlineSmall) }
            item { PacklyProgress(trip.entries.count { it.isPacked }, trip.entries.size) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                    PackingFilter.entries.forEach { chip ->
                        FilterChip(selected = filter == chip, onClick = { filter = chip }, label = { Text(chip.name) })
                    }
                }
            }
            doc.categories.sortedBy { it.sortOrder }.forEach { category ->
                val section = visible.filter { it.categoryIdSnapshot == category.id }.sortedBy { it.sortOrder }
                if (section.isNotEmpty()) {
                    item(key = "packing_header_${category.key}") { CategoryHeader(category, "${section.count { it.isPacked }}/${section.size}") }
                    items(section, key = { it.id }) { entry -> PackingItemRow(entry, category, onToggle = { onToggle(entry.id) }) }
                }
            }
            if (visible.isEmpty()) item { EmptyState("No ${filter.name.lowercase()} items.", "Everything in this view is packed or filtered out.") }
            if (trip.entries.isNotEmpty() && trip.entries.all { it.isPacked }) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(PacklySpacing.marginMobile)) {
                            Text("Everything is packed. Safe travels!", style = MaterialTheme.typography.titleLarge)
                            Text("Review packed items or go back to your trip.")
                        }
                    }
                }
            }
        }
    }
}
