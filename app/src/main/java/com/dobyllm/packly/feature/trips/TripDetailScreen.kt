@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.ui.component.PacklyProgress

@Composable
fun TripDetailScreen(
    doc: PacklyAppDocument,
    tripId: TripId,
    onBack: () -> Unit,
    onPack: () -> Unit,
    onReset: () -> Unit,
    onQuantityChange: (TripEntryId, Int) -> Unit,
    onDeadlineChange: (InstantString?) -> Unit,
) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var showResetConfirm by remember(tripId) { mutableStateOf(false) }
    var deadlineInput by remember(trip?.packBy) { mutableStateOf(PacklyDeadlineFormatter.formatInput(trip?.packBy)) }
    var notificationPermissionGranted by rememberNotificationPermissionState()
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    val parsedDeadline = remember(deadlineInput) { PacklyDeadlineFormatter.parseLocalInput(deadlineInput) }
    val deadlineInvalid = deadlineInput.isNotBlank() && parsedDeadline == null
    val hasDeadlineChange = trip != null && parsedDeadline != trip.packBy && !(deadlineInput.isBlank() && trip.packBy == null)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(trip?.name ?: "Trip") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }) },
    ) { padding ->
        if (trip == null) {
            Text("Trip not found", Modifier.padding(padding).padding(20.dp))
        } else {
            val unpackedItems = trip.entries.count { !it.isPacked }
            val deadlineWarning = unpackedItems > 0 && PacklyDeadlineFormatter.isCloseOrOverdue(trip.packBy)
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                trip.destination.takeIf { it.isNotBlank() }?.let { destination ->
                    item { Text(destination, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                item { PacklyProgress(trip.entries.count { it.isPacked }, trip.entries.size) }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Pack by", style = MaterialTheme.typography.titleMedium)
                            PacklyDeadlineFormatter.formatDisplay(trip.packBy)?.let { packBy ->
                                Text(
                                    if (deadlineWarning) "Reminder: pack by $packBy — $unpackedItems item(s) still unpacked" else "Pack by $packBy",
                                    color = if (deadlineWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = deadlineInput,
                                onValueChange = { deadlineInput = it },
                                label = { Text("Pack by date/time") },
                                placeholder = { Text(PacklyDeadlineFormatter.InputPattern) },
                                supportingText = {
                                    Text(
                                        when {
                                            deadlineInvalid -> "Use ${PacklyDeadlineFormatter.InputPattern}."
                                            deadlineInput.isNotBlank() && !notificationPermissionGranted -> "Notifications are off. We can still show the deadline in Packly."
                                            else -> "We'll remind you if anything is still unpacked."
                                        },
                                    )
                                },
                                isError = deadlineInvalid,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !deadlineInvalid && hasDeadlineChange,
                                    onClick = {
                                        if (deadlineInput.isNotBlank() && !notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        onDeadlineChange(parsedDeadline)
                                    },
                                ) { Text("Save deadline") }
                                OutlinedButton(
                                    enabled = trip.packBy != null || deadlineInput.isNotBlank(),
                                    onClick = {
                                        deadlineInput = ""
                                        onDeadlineChange(null)
                                    },
                                ) { Text("Clear") }
                            }
                        }
                    }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onPack) { Text("Start packing") }; OutlinedButton(onClick = { showResetConfirm = true }) { Text("Reset packed items") } } }
                item { Text("Trip quantities", style = MaterialTheme.typography.titleMedium) }
                items(trip.entries, key = { it.id }) { entry ->
                    val category = doc.categories.firstOrNull { it.id == entry.categoryIdSnapshot }?.label ?: "Unknown"
                    ListItem(
                        headlineContent = { Text(entry.nameSnapshot) },
                        supportingContent = { Text(category) },
                        trailingContent = {
                            QuantityStepper(
                                name = entry.nameSnapshot,
                                quantity = entry.quantity,
                                onQuantityChange = { onQuantityChange(entry.id, it) },
                            )
                        },
                    )
                }
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
private fun QuantityStepper(name: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $name quantity")
        }
        Text("×$quantity", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onQuantityChange(quantity + 1) }) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $name quantity")
        }
    }
}
