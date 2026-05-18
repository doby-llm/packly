package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Trip
import com.packly.app.data.model.TripEntry
import com.packly.app.data.repository.PacklyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    repository: PacklyRepository,
    onBack: () -> Unit
) {
    val trips by repository.getTrips().collectAsState(initial = emptyList())
    val items by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    val trip = trips.find { it.id == tripId }
    val itemMap = remember(items) { items.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Trip") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (trip == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Trip not found")
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Trip info header
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(trip.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Date: ${trip.date}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${trip.items.size} items (${trip.items.count { it.packed }}/${trip.items.size} packed)", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Items checklist
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trip.items, key = { it.itemId }) { entry ->
                        val item = itemMap[entry.itemId]
                        TripItemRow(entry = entry, itemName = item?.name ?: entry.itemId)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripItemRow(
    entry: TripEntry,
    itemName: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "x${entry.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
