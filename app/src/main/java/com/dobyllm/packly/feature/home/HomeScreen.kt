@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument

@Composable
fun HomeScreen(doc: PacklyAppDocument, onItems: () -> Unit, onLists: () -> Unit, onTrips: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ready to pack?", style = MaterialTheme.typography.headlineMedium)
            Text("Saved on this device. Start with sample items or create your own trip.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            DestinationCard(Icons.Rounded.EditNote, "Edit items", "${doc.items.count { !it.isArchived }} reusable packing items", onItems)
            DestinationCard(Icons.Rounded.Checklist, "Item Lists", "${doc.lists.count { !it.isArchived }} reusable templates", onLists)
            DestinationCard(Icons.Rounded.Backpack, "Trips", "${doc.trips.count { it.status.name != "Archived" }} packing sessions", onTrips)
        }
    }
}

@Composable
private fun DestinationCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Column { Text(title, style = MaterialTheme.typography.titleLarge); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
