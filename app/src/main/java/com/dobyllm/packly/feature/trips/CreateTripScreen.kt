@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CreateTripSheet(
    doc: PacklyAppDocument,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<ListId>, Set<ItemId>, Map<ItemId, Int>, InstantString?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var packByDeadline by remember { mutableStateOf<InstantString?>(null) }
    var reminderDraftIncomplete by remember { mutableStateOf(false) }
    val selectedSourceListIds = remember { mutableStateListOf<ListId>() }
    var itemQuery by remember { mutableStateOf("") }
    var notificationPermissionGranted by rememberNotificationPermissionState()
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    val selectedItems = remember { mutableStateListOf<ItemId>() }
    val quantities = remember { mutableStateMapOf<ItemId, Int>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val activeLists = doc.lists.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.trips.any { it.status != TripStatus.Archived && it.name.equals(name.trim(), ignoreCase = true) }
    val selectedLists = selectedSourceListIds.mapNotNull { listId -> activeLists.firstOrNull { it.id == listId } }
    val sourceEntries = selectedLists.flatMap { list -> list.entries.sortedBy { it.sortOrder } }
    val selectedItemIds = remember(sourceEntries, selectedItems.toList()) { sourceEntries.mapNotNull { it.itemId }.toSet() + selectedItems }
    val sourceItemIds = sourceEntries.mapNotNull { it.itemId }.toSet()
    val duplicateAcrossListsCount = sourceEntries.mapNotNull { it.itemId }.let { ids -> ids.size - ids.toSet().size }
    val duplicateIndividualCount = selectedItems.count { it in sourceItemIds }
    val duplicateSourceCount = duplicateAcrossListsCount + duplicateIndividualCount
    val reviewItems = remember(selectedItemIds, doc.items, sourceEntries) {
        buildTripReviewItems(selectedItemIds, doc.items, sourceEntries)
    }
    val trimmedName = name.trim()
    val isTripNameMissing = trimmedName.isEmpty()
    val canSaveTrip = !isTripNameMissing && !duplicateName && !reminderDraftIncomplete

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
                stringResource(R.string.create_trip_title),
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
                    .padding(horizontal = PacklySpacing.md)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            ) {
                PacklyTripTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.field_trip_name_required),
                    supportingText = when {
                        duplicateName -> stringResource(R.string.error_duplicate_trip_name)
                        isTripNameMissing -> stringResource(R.string.trip_name_required_enable)
                        else -> stringResource(R.string.field_required)
                    },
                    isError = duplicateName,
                    singleLine = true,
                )
                if (showDestination) {
                    PacklyTripTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = stringResource(R.string.field_destination_optional),
                    )
                } else {
                    TextButton(onClick = { showDestination = true }) { Text(stringResource(R.string.action_add_destination_optional)) }
                }
                PacklyDeadlinePickerField(
                    deadline = packByDeadline,
                    onDeadlineChange = { packByDeadline = it },
                    onIncompleteChange = { reminderDraftIncomplete = it },
                    supportingText = when {
                        reminderDraftIncomplete -> stringResource(R.string.deadline_choose_time_supporting)
                        packByDeadline != null && !notificationPermissionGranted -> stringResource(R.string.deadline_notifications_off)
                        else -> stringResource(R.string.deadline_supporting_text)
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.section_start_from_lists), style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(R.string.selected_count_label, selectedSourceListIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.start_from_lists_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (activeLists.isEmpty()) {
                        Text(
                            stringResource(R.string.no_packing_lists_yet_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                            verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                        ) {
                            activeLists.forEach { list ->
                                val selected = list.id in selectedSourceListIds
                                PacklyTripFilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedSourceListIds.remove(list.id) else selectedSourceListIds.add(list.id)
                                    },
                                    label = stringResource(R.string.category_count_label, list.name, list.entries.size),
                                )
                            }
                        }
                    }
                    if (selectedLists.isEmpty()) {
                        Text(
                            stringResource(R.string.no_lists_selected_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(stringResource(R.string.selected_lists_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                            verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                        ) {
                            selectedLists.forEach { list ->
                                PacklyTripFilterChip(
                                    selected = true,
                                    onClick = { selectedSourceListIds.remove(list.id) },
                                    label = stringResource(R.string.selected_list_remove_label, list.name),
                                )
                            }
                        }
                    }
                }
                Text(stringResource(R.string.section_add_individual_items), style = MaterialTheme.typography.labelLarge)
                PacklyTripTextField(
                    value = itemQuery,
                    onValueChange = { itemQuery = it },
                    label = stringResource(R.string.search_all_items_count_label, activeItems.size),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    matchingItems.forEach { item ->
                        val alreadyIncludedFromList = item.id in sourceItemIds
                        PacklyTripFilterChip(
                            selected = item.id in selectedItems || alreadyIncludedFromList,
                            onClick = { if (item.id in selectedItems) selectedItems.remove(item.id) else selectedItems.add(item.id) },
                            label = if (alreadyIncludedFromList) {
                                stringResource(R.string.item_already_included_label, item.name, stringResource(R.string.already_included_suffix))
                            } else {
                                item.name
                            },
                        )
                    }
                }
                if (matchingItems.isEmpty()) Text(stringResource(R.string.no_items_match_search), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (duplicateSourceCount > 0) {
                    Text(
                        stringResource(R.string.duplicate_source_warning, duplicateSourceCount),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (reviewItems.isNotEmpty()) {
                    Text(stringResource(R.string.section_review_quantities), style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                        reviewItems.forEach { reviewItem ->
                            val category = doc.categories.firstOrNull { it.id == reviewItem.categoryId }?.label ?: stringResource(R.string.unknown_category)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = PacklySpacing.md, vertical = PacklySpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        if (!canSaveTrip) {
                            Text(
                                text = when {
                                    isTripNameMissing -> stringResource(R.string.create_trip_disabled_missing_name)
                                    duplicateName -> stringResource(R.string.create_trip_disabled_duplicate_name)
                                    else -> stringResource(R.string.create_trip_disabled_time_required)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            enabled = canSaveTrip,
                            onClick = {
                                if (packByDeadline != null && !notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onCreate(name, destination, selectedSourceListIds.toList(), selectedItems.toSet(), quantities.toMap(), packByDeadline)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(PacklyRadius.default),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) { Text(stringResource(R.string.action_create_trip)) }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberNotificationPermissionState(): MutableState<Boolean> {
    val context = LocalContext.current
    return remember { mutableStateOf(canPostPacklyNotifications(context)) }
}

@Composable
internal fun PacklyDeadlinePickerField(
    deadline: InstantString?,
    onDeadlineChange: (InstantString?) -> Unit,
    supportingText: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onIncompleteChange: (Boolean) -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val selectedDate = PacklyDeadlineFormatter.localDate(deadline)
    val selectedTime = PacklyDeadlineFormatter.localTime(deadline) ?: LocalTime.now()
    val hasIncompleteDraft = pendingDate != null
    val selectedDisplay = when {
        hasIncompleteDraft -> stringResource(R.string.deadline_choose_time_display)
        deadline == null -> stringResource(R.string.deadline_no_reminder_set)
        else -> PacklyDeadlineFormatter.formatDisplay(deadline) ?: stringResource(R.string.deadline_no_reminder_set)
    }

    LaunchedEffect(hasIncompleteDraft) {
        onIncompleteChange(hasIncompleteDraft)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.default),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(PacklySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            Text(label ?: stringResource(R.string.deadline_pack_by_optional), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = selectedDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    hasIncompleteDraft -> MaterialTheme.colorScheme.error
                    deadline == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasIncompleteDraft) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(PacklyRadius.default),
                ) {
                    Text(if (deadline == null && !hasIncompleteDraft) stringResource(R.string.action_choose_date) else stringResource(R.string.action_change))
                }
                OutlinedButton(
                    enabled = deadline != null || hasIncompleteDraft,
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(PacklyRadius.default),
                ) { Text(if (hasIncompleteDraft) stringResource(R.string.action_choose_time) else stringResource(R.string.action_change_time)) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = deadline != null || hasIncompleteDraft,
                    onClick = {
                        pendingDate = null
                        onDeadlineChange(null)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 48.dp),
                ) { Text(stringResource(R.string.action_clear), maxLines = 1, softWrap = false) }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineDateMillis(pendingDate ?: selectedDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = datePickerState.selectedDateMillis?.toLocalDateUtc() ?: LocalDate.now()
                        pendingDate = date
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) { Text(stringResource(R.string.action_next_choose_time)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.deadline_time_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pendingDate ?: PacklyDeadlineFormatter.localDate(deadline) ?: LocalDate.now()
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onDeadlineChange(PacklyDeadlineFormatter.toInstantString(date, time))
                        pendingDate = null
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.action_save_time)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun deadlineDateMillis(date: LocalDate?): Long? = date
    ?.atStartOfDay(ZoneOffset.UTC)
    ?.toInstant()
    ?.toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

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
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.a11y_decrease_quantity, name))
                }
                Text(stringResource(R.string.quantity_times, quantity), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.a11y_increase_quantity, name))
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
