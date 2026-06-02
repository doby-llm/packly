@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun HomeScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItems: () -> Unit,
    onLists: () -> Unit,
    onTrips: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.marginMobile)) {
            Text("Ready to pack?", style = MaterialTheme.typography.headlineMedium)
            Text("Choose what you want to prepare.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            DestinationCard(
                icon = Icons.Rounded.Backpack,
                title = "Trips",
                body = "${doc.trips.count { it.status.name != "Archived" }} packing sessions",
                onClick = onTrips,
            )
            DestinationCard(
                icon = Icons.Rounded.Checklist,
                title = "Lists",
                body = "${doc.lists.count { !it.isArchived }} reusable templates",
                onClick = onLists,
            )
            DestinationCard(
                icon = Icons.Rounded.EditNote,
                title = "Items",
                body = "${doc.items.count { !it.isArchived }} reusable packing items",
                onClick = onItems,
            )
        }
    }
}

@Composable
private fun DestinationCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
