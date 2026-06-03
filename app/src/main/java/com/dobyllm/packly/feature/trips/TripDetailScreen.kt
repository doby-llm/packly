@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.TripEntryId
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.TripSummaryCard
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import kotlin.math.roundToInt

@Composable
fun TripDetailScreen(
    doc: PacklyAppDocument,
    tripId: TripId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPack: () -> Unit,
    onReset: () -> Unit,
    onQuantityChange: (TripEntryId, Int) -> Unit,
    onDeadlineChange: (InstantString?) -> Unit,
) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var showResetConfirm by remember(tripId) { mutableStateOf(false) }
    var deadlineDraft by remember(trip?.packBy) { mutableStateOf(trip?.packBy) }
    var notificationPermissionGranted by rememberNotificationPermissionState()
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    val hasDeadlineChange = trip != null && deadlineDraft != trip.packBy

    if (trip == null) {
        EmptyState(
            title = "This trip could not be found",
            body = "Go back to Trips and choose another trip.",
            modifier = Modifier.padding(contentPadding).padding(PacklySpacing.md),
        )
    } else {
        val packedItems = trip.entries.count { it.isPacked }
        val totalItems = trip.entries.size
        val progress = packedItems / totalItems.coerceAtLeast(1).toFloat()
        val unpackedItems = totalItems - packedItems
        val deadlineWarning = unpackedItems > 0 && PacklyDeadlineFormatter.isCloseOrOverdue(trip.packBy)
        val packBy = PacklyDeadlineFormatter.formatDisplay(trip.packBy)
        val dateRange = listOfNotNull(trip.startDate, trip.endDate).joinToString(" - ").ifBlank { null }
        val metadata = listOfNotNull(
            dateRange,
            trip.destination.takeIf { it.isNotBlank() },
            "$packedItems/$totalItems packed",
            packBy?.let { "Pack by $it" },
        ).joinToString(" • ")
        val chips = listOfNotNull(
            trip.destination.takeIf { it.isNotBlank() },
            packBy?.let { "Pack by $it" },
            totalItems.takeIf { it > 0 }?.let { "$it items" },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = trip.destination.takeIf { it.isNotBlank() } ?: "Trip packing details",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                TripSummaryCard(
                    title = trip.name,
                    metadata = metadata,
                    percentLabel = "${(progress * 100).roundToInt()}% Packed",
                    progress = progress,
                    chips = chips,
                    accentColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onPack,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                        Button(onClick = onPack) { Text(if (packedItems == 0) "Start packing" else "Continue packing") }
                        OutlinedButton(onClick = { showResetConfirm = true }) { Text("Reset packed items") }
                    }
                }
            }
            item {
                DeadlineCard(
                    packBy = packBy,
                    unpackedItems = unpackedItems,
                    deadlineWarning = deadlineWarning,
                    deadlineDraft = deadlineDraft,
                    onDeadlineDraftChange = { deadlineDraft = it },
                    notificationPermissionGranted = notificationPermissionGranted,
                    hasDeadlineChange = hasDeadlineChange,
                    onSaveDeadline = {
                        if (deadlineDraft != null && !notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onDeadlineChange(deadlineDraft)
                    },
                    onClearDeadline = {
                        deadlineDraft = null
                        onDeadlineChange(null)
                    },
                    canClearDeadline = trip.packBy != null || deadlineDraft != null,
                )
            }
            item { Text("Trip quantities", style = MaterialTheme.typography.titleLarge) }
            items(trip.entries, key = { it.id }) { entry ->
                val category = doc.categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.label ?: "Unknown"
                TripQuantityRow(
                    name = entry.nameSnapshot,
                    category = category,
                    quantity = entry.quantity,
                    isPacked = entry.isPacked,
                    onQuantityChange = { onQuantityChange(entry.id, it) },
                )
            }
        }
    }

    if (trip != null && showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset packed items?") },
            text = { Text("All packed checkmarks for ${trip.name} will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DeadlineCard(
    packBy: String?,
    unpackedItems: Int,
    deadlineWarning: Boolean,
    deadlineDraft: InstantString?,
    onDeadlineDraftChange: (InstantString?) -> Unit,
    notificationPermissionGranted: Boolean,
    hasDeadlineChange: Boolean,
    onSaveDeadline: () -> Unit,
    onClearDeadline: () -> Unit,
    canClearDeadline: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Text("Pack by", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            packBy?.let { displayValue ->
                Text(
                    if (deadlineWarning) {
                        "Reminder: pack by $displayValue — $unpackedItems item(s) still unpacked"
                    } else {
                        "Pack by $displayValue"
                    },
                    color = if (deadlineWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PacklyDeadlinePickerField(
                deadline = deadlineDraft,
                onDeadlineChange = onDeadlineDraftChange,
                label = "Pack by date/time",
                supportingText = when {
                    deadlineDraft != null && !notificationPermissionGranted -> "Notifications are off. We can still show the deadline in Packly."
                    else -> "Pick a date, then adjust the optional reminder time. Default time is 18:00."
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                Button(
                    enabled = hasDeadlineChange,
                    onClick = onSaveDeadline,
                    shape = RoundedCornerShape(PacklyRadius.default),
                ) { Text("Save deadline") }
                OutlinedButton(
                    enabled = canClearDeadline,
                    onClick = onClearDeadline,
                    modifier = Modifier.widthIn(min = 72.dp),
                    shape = RoundedCornerShape(PacklyRadius.default),
                ) { Text("Clear", maxLines = 1, softWrap = false) }
            }
        }
    }
}

@Composable
private fun TripQuantityRow(
    name: String,
    category: String,
    quantity: Int,
    isPacked: Boolean,
    onQuantityChange: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PacklySpacing.md, vertical = PacklySpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(category, if (isPacked) "Packed" else "Unpacked").joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease $name quantity")
                }
                Text("×$quantity", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase $name quantity")
                }
            }
        }
    }
}
