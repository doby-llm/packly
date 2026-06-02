@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.notification.canPostPacklyNotifications

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CreateTripSheet(
    doc: PacklyAppDocument,
    onDismiss: () -> Unit,
    onCreate: (String, String, ListId?, Set<ItemId>, Map<ItemId, Int>, InstantString?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var packByInput by remember { mutableStateOf("") }
    var sourceListId by remember { mutableStateOf<ListId?>(null) }
    var itemQuery by remember { mutableStateOf("") }
    var notificationPermissionGranted by rememberNotificationPermissionState()
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    val selectedItems = remember { mutableStateListOf<ItemId>() }
    val quantities = remember { mutableStateMapOf<ItemId, Int>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.trips.any { it.status != TripStatus.Archived && it.name.equals(name.trim(), ignoreCase = true) }
    val sourceEntries = sourceListId
        ?.let { id -> doc.lists.firstOrNull { it.id == id }?.entries }
        ?: emptyList()
    val selectedItemIds = remember(sourceEntries, selectedItems.toList()) { sourceEntries.mapNotNull { it.itemId }.toSet() + selectedItems }
    val sourceItemIds = sourceEntries.mapNotNull { it.itemId }.toSet()
    val duplicateSourceCount = selectedItems.count { it in sourceItemIds }
    val reviewItems = remember(selectedItemIds, doc.items, sourceEntries) {
        buildTripReviewItems(selectedItemIds, doc.items, sourceEntries)
    }
    val parsedPackBy = remember(packByInput) { PacklyDeadlineFormatter.parseLocalInput(packByInput) }
    val packByInvalid = packByInput.isNotBlank() && parsedPackBy == null

    LaunchedEffect(selectedItemIds) {
        selectedItemIds.forEach { itemId -> quantities.putIfAbsent(itemId, 1) }
        quantities.keys.toList().filterNot { it in selectedItemIds }.forEach { quantities.remove(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding().imePadding()) {
            Text(
                "Start trip",
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Trip name") },
                supportingText = { if (duplicateName) Text("An active trip with this name already exists.") },
                isError = duplicateName,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showDestination) {
                OutlinedTextField(
                    destination,
                    { destination = it },
                    label = { Text("Destination (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { showDestination = true }) { Text("Add destination (optional)") }
            }
            OutlinedTextField(
                value = packByInput,
                onValueChange = { packByInput = it },
                label = { Text("Pack by (optional)") },
                placeholder = { Text(PacklyDeadlineFormatter.InputPattern) },
                supportingText = {
                    Text(
                        when {
                            packByInvalid -> "Use ${PacklyDeadlineFormatter.InputPattern}. We'll remind you if anything is still unpacked."
                            packByInput.isNotBlank() && !notificationPermissionGranted -> "Notifications are off. We can still show the deadline in Packly."
                            else -> "We'll remind you if anything is still unpacked."
                        },
                    )
                },
                isError = packByInvalid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Use a list", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sourceListId == null, onClick = { sourceListId = null }, label = { Text("Blank") })
                doc.lists.filterNot { it.isArchived }.forEach { list -> FilterChip(selected = sourceListId == list.id, onClick = { sourceListId = list.id }, label = { Text(list.name) }) }
            }
            Text("Add isolated items", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(itemQuery, { itemQuery = it }, label = { Text("Search all ${activeItems.size} items") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                matchingItems.forEach { item ->
                    FilterChip(
                        selected = item.id in selectedItems,
                        onClick = { if (item.id in selectedItems) selectedItems.remove(item.id) else selectedItems.add(item.id) },
                        label = { Text(item.name) },
                    )
                }
            }
            if (matchingItems.isEmpty()) Text("No items match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (duplicateSourceCount > 0) {
                Text(
                    "$duplicateSourceCount selected item(s) already exist in the source list and will be included only once.",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (reviewItems.isNotEmpty()) {
                Text("Review quantities", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reviewItems.forEach { reviewItem ->
                        val category = doc.categories.firstOrNull { it.id == reviewItem.categoryId }?.label ?: "Unknown"
                        QuantityReviewRow(
                            name = reviewItem.name,
                            category = category,
                            quantity = quantities[reviewItem.itemId] ?: 1,
                            onQuantityChange = { quantity -> quantities[reviewItem.itemId] = quantity.coerceAtLeast(1) },
                        )
                    }
                }
            }
            }
            Button(
                enabled = name.trim().isNotEmpty() && !duplicateName && !packByInvalid,
                onClick = {
                    if (packByInput.isNotBlank() && !notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onCreate(name, destination, sourceListId, selectedItems.toSet(), quantities.toMap(), parsedPackBy)
                    onDismiss()
                },
                modifier = Modifier.padding(20.dp).fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) { Text("Save trip") }
        }
    }
}

@Composable
fun rememberNotificationPermissionState(): MutableState<Boolean> {
    val context = LocalContext.current
    return remember { mutableStateOf(canPostPacklyNotifications(context)) }
}

private data class TripReviewItem(
    val itemId: ItemId,
    val name: String,
    val categoryId: CategoryId,
    val sortOrder: Int,
)

private fun buildTripReviewItems(
    selectedItemIds: Set<ItemId>,
    items: List<PacklyItem>,
    sourceEntries: List<PacklyListEntry>,
): List<TripReviewItem> {
    val sourceReviewItems = sourceEntries.mapNotNull { entry ->
        val itemId = entry.itemId ?: return@mapNotNull null
        if (itemId !in selectedItemIds) return@mapNotNull null
        TripReviewItem(itemId, entry.itemNameSnapshot, entry.categoryIdSnapshot, entry.sortOrder)
    }
    val sourceIds = sourceReviewItems.map { it.itemId }.toSet()
    val isolatedReviewItems = items
        .filter { item -> item.id in selectedItemIds && item.id !in sourceIds }
        .mapIndexed { index, item -> TripReviewItem(item.id, item.name, item.categoryId, sourceEntries.size + index) }
    return (sourceReviewItems + isolatedReviewItems).distinctBy { it.itemId }.sortedBy { it.sortOrder }
}

@Composable
private fun QuantityReviewRow(name: String, category: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
