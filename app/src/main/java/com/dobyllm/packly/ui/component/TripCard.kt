package com.dobyllm.packly.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter

@Composable
fun TripCard(trip: PacklyTrip, onOpen: () -> Unit, onPack: () -> Unit, onDelete: () -> Unit) {
    val packed = trip.entries.count { it.isPacked }
    val total = trip.entries.size.coerceAtLeast(1)
    val unpackedItems = trip.entries.count { !it.isPacked }
    val packBy = PacklyDeadlineFormatter.formatDisplay(trip.packBy)
    val deadlineWarning = unpackedItems > 0 && PacklyDeadlineFormatter.isCloseOrOverdue(trip.packBy)
    val metadata = listOfNotNull(
        trip.destination.takeIf { it.isNotBlank() },
        "$packed/${trip.entries.size} packed",
        packBy?.let { if (deadlineWarning) "Reminder: Pack by $it" else "Pack by $it" },
    ).joinToString(" • ")

    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(trip.name, style = MaterialTheme.typography.titleMedium)
            Text(
                metadata,
                color = if (deadlineWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = metadata },
            )
            if (deadlineWarning) {
                AssistChip(
                    onClick = onOpen,
                    label = { Text("Pack-by reminder due soon with unpacked items") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
                )
            }
            LinearProgressIndicator(progress = { packed / total.toFloat() }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPack) { Text(if (packed == 0) "Start packing" else "Continue packing") }
                OutlinedButton(onClick = onOpen) { Text("Details") }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    }
}
