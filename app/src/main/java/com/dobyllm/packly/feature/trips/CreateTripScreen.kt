@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.model.TripStatus
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.notification.canPostPacklyNotifications
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding().imePadding()) {
            Text(
                "Create trip",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = PacklySpacing.md)
                    .padding(top = PacklySpacing.base, bottom = PacklySpacing.sm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PacklySpacing.md),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            ) {
                PacklyTripTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Trip name",
                    supportingText = if (duplicateName) "An active trip with this name already exists." else null,
                    isError = duplicateName,
                )
                if (showDestination) {
                    PacklyTripTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = "Destination (optional)",
                    )
                } else {
                    TextButton(onClick = { showDestination = true }) { Text("Add destination (optional)") }
                }
                PacklyTripTextField(
                    value = packByInput,
                    onValueChange = { packByInput = it },
                    label = "Pack by (optional)",
                    placeholder = PacklyDeadlineFormatter.InputPattern,
                    supportingText = when {
                        packByInvalid -> "Use ${PacklyDeadlineFormatter.InputPattern}. We'll remind you if anything is still unpacked."
                        packByInput.isNotBlank() && !notificationPermissionGranted -> "Notifications are off. We can still show the deadline in Packly."
                        else -> "We'll remind you if anything is still unpacked."
                    },
                    isError = packByInvalid,
                    singleLine = true,
                )
                Text("Use a list", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    PacklyTripFilterChip(selected = sourceListId == null, onClick = { sourceListId = null }, label = "Blank")
                    doc.lists.filterNot { it.isArchived }.forEach { list ->
                        PacklyTripFilterChip(
                            selected = sourceListId == list.id,
                            onClick = { sourceListId = list.id },
                            label = list.name,
                        )
                    }
                }
                Text("Add isolated items", style = MaterialTheme.typography.labelLarge)
                PacklyTripTextField(
                    value = itemQuery,
                    onValueChange = { itemQuery = it },
                    label = "Search all ${activeItems.size} items",
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    matchingItems.forEach { item ->
                        PacklyTripFilterChip(
                            selected = item.id in selectedItems,
                            onClick = { if (item.id in selectedItems) selectedItems.remove(item.id) else selectedItems.add(item.id) },
                            label = item.name,
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
                    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
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
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 560.dp)
                    .padding(PacklySpacing.md)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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

@Composable
internal fun PacklyTripTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.default),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    )
}

@Composable
internal fun PacklyTripFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(PacklyRadius.default),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            selectedContainerColor = MaterialTheme.colorScheme.primaryFixed,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
        ),
    )
}
