@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.packing

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
import com.dobyllm.packly.ui.component.CategoryHeader
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.PackingItemRow
import com.dobyllm.packly.ui.component.PacklyProgress

private enum class PackingFilter { All, Unpacked, Packed }

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PackingModeScreen(doc: PacklyAppDocument, tripId: TripId, onBack: () -> Unit, onToggle: (TripEntryId) -> Unit) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var filter by remember { mutableStateOf(PackingFilter.All) }
    Scaffold(topBar = { TopAppBar(title = { Text("Packing") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        if (trip == null) Text("Trip not found", Modifier.padding(padding).padding(20.dp)) else {
            val visible = trip.entries.filter { when (filter) { PackingFilter.All -> true; PackingFilter.Unpacked -> !it.isPacked; PackingFilter.Packed -> it.isPacked } }
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(trip.name, style = MaterialTheme.typography.headlineSmall) }
                item { PacklyProgress(trip.entries.count { it.isPacked }, trip.entries.size) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PackingFilter.entries.forEach { chip -> FilterChip(selected = filter == chip, onClick = { filter = chip }, label = { Text(chip.name) }) } } }
                doc.categories.sortedBy { it.sortOrder }.forEach { category ->
                    val section = visible.filter { it.categoryIdSnapshot == category.id }.sortedBy { it.sortOrder }
                    if (section.isNotEmpty()) {
                        item(key = "packing_header_${category.key}") { CategoryHeader(category, "${section.count { it.isPacked }}/${section.size}") }
                        items(section, key = { it.id }) { entry -> PackingItemRow(entry, category, onToggle = { onToggle(entry.id) }) }
                    }
                }
                if (visible.isEmpty()) item { EmptyState("No ${filter.name.lowercase()} items.", "Everything in this view is packed or filtered out.") }
                if (trip.entries.isNotEmpty() && trip.entries.all { it.isPacked }) item { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Everything is packed. Safe travels!", style = MaterialTheme.typography.titleLarge); Text("Review packed items or go back to your trip.") } } }
            }
        }
    }
}
