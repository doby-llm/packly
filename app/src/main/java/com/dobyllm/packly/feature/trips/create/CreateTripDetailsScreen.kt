@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dobyllm.packly.feature.trips.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.notification.canPostPacklyNotifications
import com.dobyllm.packly.ui.i18n.displayDescription
import com.dobyllm.packly.ui.i18n.displayItemNameSnapshot
import com.dobyllm.packly.ui.i18n.displayLabel
import com.dobyllm.packly.ui.i18n.displayName
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private const val CREATE_TRIP_TOTAL_STEPS = 4

@Composable
fun CreateTripDetailsScreen(
    doc: PacklyAppDocument,
    draftState: CreateTripDraftState,
    contentPadding: PaddingValues,
    onNext: () -> Unit,
    onCloseConfirmed: () -> Unit,
) {
    val trimmedName = draftState.name.trim()
    val isNameMissing = trimmedName.isEmpty()
    val isDuplicateName = draftState.duplicateNameIn(doc)
    val canContinue = !isNameMissing && !isDuplicateName

    BackHandler { draftState.requestClose(onCloseConfirmed) }

    CreateTripStepScaffold(
        step = 1,
        title = stringResource(R.string.create_trip_step1_title),
        body = stringResource(R.string.create_trip_step1_body),
        contentPadding = contentPadding,
        footer = {
            if (!canContinue) DisabledReason(if (isDuplicateName) R.string.create_trip_disabled_duplicate_name else R.string.create_trip_disabled_missing_name)
            Button(
                enabled = canContinue,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(stringResource(R.string.action_next)) }
        },
    ) {
        item {
            CreateTripTextField(
                value = draftState.name,
                onValueChange = draftState::updateName,
                label = stringResource(R.string.create_trip_name_label),
                placeholder = stringResource(R.string.create_trip_name_placeholder),
                supportingText = when {
                    isDuplicateName -> stringResource(R.string.error_duplicate_trip_name)
                    isNameMissing -> stringResource(R.string.create_trip_name_required_supporting)
                    else -> stringResource(R.string.field_required)
                },
                isError = isDuplicateName,
                singleLine = true,
            )
        }
        item {
            CreateTripTextField(
                value = draftState.destination,
                onValueChange = draftState::updateDestination,
                label = stringResource(R.string.create_trip_destination_label_optional),
                placeholder = stringResource(R.string.create_trip_destination_placeholder),
                singleLine = true,
            )
        }
    }

    CreateTripDiscardDialog(draftState = draftState, onCloseConfirmed = onCloseConfirmed)
}

@Composable
fun CreateTripDeadlineScreen(
    draftState: CreateTripDraftState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    BackHandler(onBack = onBack)
    CreateTripStepScaffold(
        step = 2,
        title = stringResource(R.string.create_trip_step2_title),
        body = stringResource(R.string.create_trip_step2_body),
        contentPadding = contentPadding,
        footer = {
            if (draftState.reminderDraftIncomplete) DisabledReason(R.string.create_trip_disabled_time_required)
            StepFooterButtons(
                onBack = onBack,
                nextLabel = stringResource(R.string.action_continue),
                nextEnabled = !draftState.reminderDraftIncomplete,
                onNext = onNext,
            )
        },
    ) {
        item {
            PackByPickerCard(
                deadline = draftState.packBy,
                onDeadlineChange = draftState::updatePackBy,
                onIncompleteChange = draftState::updateReminderDraftIncomplete,
                onClear = draftState::clearPackBy,
            )
        }
    }
}

@Composable
fun CreateTripListsScreen(
    doc: PacklyAppDocument,
    draftState: CreateTripDraftState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val activeLists = doc.lists.filterNot { it.isArchived }
    val selectedEntryIds = draftState.selectedListEntryIds
    BackHandler(onBack = onBack)
    CreateTripStepScaffold(
        step = 3,
        title = stringResource(R.string.create_trip_step3_title),
        body = stringResource(R.string.create_trip_step3_body),
        contentPadding = contentPadding,
        footer = {
            Text(
                text = stringResource(R.string.selected_count_label, activeLists.count { list -> list.entries.any { it.id in selectedEntryIds } }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepFooterButtons(
                onBack = onBack,
                nextLabel = stringResource(R.string.action_next),
                nextEnabled = true,
                onNext = onNext,
            )
        },
    ) {
        if (activeLists.isEmpty()) {
            item { EmptyStateCard(R.string.create_trip_lists_empty_title, R.string.create_trip_lists_empty_body) }
        } else {
            items(activeLists, key = { it.id }) { list ->
                val listEntries = list.entries.sortedBy(PacklyListEntry::sortOrder)
                ListSelectionCard(
                    list = list,
                    entries = listEntries,
                    selectedEntryIds = selectedEntryIds,
                    onToggleList = { draftState.toggleSourceList(list.id, listEntries) },
                    onToggleEntry = { entryId -> draftState.toggleSourceListEntry(list.id, entryId, listEntries.mapTo(mutableSetOf()) { it.id }) },
                )
            }
        }
    }
}

@Composable
fun CreateTripItemsScreen(
    doc: PacklyAppDocument,
    draftState: CreateTripDraftState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    val activeItems = doc.items.filterNot { it.isArchived }
    val selectedLists = draftState.selectedSourceListIds.mapNotNull { listId -> doc.lists.firstOrNull { it.id == listId && !it.isArchived } }
    val sourceEntries = selectedLists.flatMap { list -> list.entries.sortedBy(PacklyListEntry::sortOrder).filter { it.id in draftState.selectedListEntryIds } }
    val sourceItemIds = sourceEntries.mapNotNull { it.itemId }.toSet()
    val selectedIdsForQuantity = sourceItemIds + draftState.selectedItemIds
    LaunchedEffect(selectedIdsForQuantity) { draftState.syncQuantitiesFor(selectedIdsForQuantity) }
    val filteredItems = activeItems.filter { item ->
        (selectedCategoryId == null || item.categoryId == selectedCategoryId) &&
            (query.isBlank() || item.displayName().contains(query.trim(), ignoreCase = true))
    }
    val reviewItems = buildTripReviewItems(selectedIdsForQuantity, activeItems, sourceEntries)
    val canFinish = draftState.name.trim().isNotEmpty() && !draftState.duplicateNameIn(doc) && !draftState.reminderDraftIncomplete

    BackHandler(onBack = onBack)
    CreateTripStepScaffold(
        step = 4,
        title = stringResource(R.string.create_trip_step4_title),
        body = stringResource(R.string.create_trip_step4_body),
        contentPadding = contentPadding,
        footer = {
            if (!canFinish) {
                DisabledReason(
                    when {
                        draftState.name.trim().isEmpty() -> R.string.create_trip_disabled_missing_name
                        draftState.duplicateNameIn(doc) -> R.string.create_trip_disabled_duplicate_name
                        else -> R.string.create_trip_disabled_time_required
                    },
                )
            }
            StepFooterButtons(
                onBack = onBack,
                nextLabel = stringResource(R.string.action_create_trip),
                nextEnabled = canFinish,
                onNext = onFinish,
            )
        },
    ) {
        item {
            CreateTripTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.search_items_placeholder),
                singleLine = true,
            )
        }
        item {
            CategoryFilterRow(
                categories = doc.categories.filterNot { it.isArchived }.sortedBy { it.sortOrder },
                selectedCategoryId = selectedCategoryId,
                onSelected = { selectedCategoryId = it },
            )
        }
        when {
            activeItems.isEmpty() -> item { EmptyStateCard(R.string.create_trip_items_empty_title, R.string.create_trip_items_empty_body) }
            filteredItems.isEmpty() -> item { EmptyStateCard(R.string.no_matches_title, R.string.no_matches_body) }
            else -> items(filteredItems, key = { it.id }) { item ->
                val includedFromList = item.id in sourceItemIds
                ItemSelectionCard(
                    item = item,
                    category = doc.categories.firstOrNull { it.id == item.categoryId }?.displayLabel() ?: stringResource(R.string.unknown_category),
                    selected = item.id in draftState.selectedItemIds || includedFromList,
                    includedFromList = includedFromList,
                    onToggle = { if (includedFromList) draftState.removeSourceItems(item.id, sourceEntries) else draftState.toggleItem(item.id) },
                )
            }
        }
        if (reviewItems.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.section_review_quantities),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(reviewItems, key = { it.itemId }) { reviewItem ->
                QuantityReviewRow(
                    name = reviewItem.name,
                    category = doc.categories.firstOrNull { it.id == reviewItem.categoryId }?.displayLabel() ?: stringResource(R.string.unknown_category),
                    quantity = draftState.itemQuantities[reviewItem.itemId] ?: 1,
                    onQuantityChange = { draftState.setQuantity(reviewItem.itemId, it) },
                    onRemove = { if (reviewItem.itemId in sourceItemIds) draftState.removeSourceItems(reviewItem.itemId, sourceEntries) else draftState.toggleItem(reviewItem.itemId) },
                )
            }
        }
    }
}

@Composable
private fun CreateTripStepScaffold(
    step: Int,
    title: String,
    body: String,
    contentPadding: PaddingValues,
    footer: @Composable ColumnScope.() -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 560.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = PacklySpacing.marginMobile,
                    top = PacklySpacing.sm,
                    end = PacklySpacing.marginMobile,
                    bottom = PacklySpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
            ) {
                item { CreateTripProgressHeader(step = step, title = title, body = body) }
                content()
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                    content = footer,
                )
            }
        }
    }
}

@Composable
private fun CreateTripProgressHeader(step: Int, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        Text(
            text = stringResource(R.string.create_trip_step_label, step, CREATE_TRIP_TOTAL_STEPS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs), modifier = Modifier.fillMaxWidth()) {
            repeat(CREATE_TRIP_TOTAL_STEPS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(999.dp)),
                ) {
                    if (index < step) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(PacklySpacing.xs))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PackByPickerCard(
    deadline: InstantString?,
    onDeadlineChange: (InstantString?) -> Unit,
    onIncompleteChange: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val hasIncompleteDraft = pendingDate != null
    val selectedDate = PacklyDeadlineFormatter.localDate(deadline)
    val selectedTime = PacklyDeadlineFormatter.localTime(deadline) ?: LocalTime.now().withSecond(0).withNano(0)
    val notificationsAvailable = canPostPacklyNotifications(androidx.compose.ui.platform.LocalContext.current)
    val selectedDisplay = when {
        hasIncompleteDraft -> stringResource(R.string.create_trip_pack_by_incomplete)
        deadline == null -> stringResource(R.string.deadline_no_reminder_set)
        else -> PacklyDeadlineFormatter.formatDisplay(deadline) ?: stringResource(R.string.deadline_no_reminder_set)
    }
    LaunchedEffect(hasIncompleteDraft) { onIncompleteChange(hasIncompleteDraft) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(PacklySpacing.marginMobile), verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
            Text(stringResource(R.string.create_trip_pack_by_time_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(selectedDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    hasIncompleteDraft -> stringResource(R.string.create_trip_pack_by_incomplete)
                    deadline != null && notificationsAvailable -> stringResource(R.string.create_trip_reminders_body_enabled)
                    deadline != null -> stringResource(R.string.deadline_notifications_off)
                    else -> stringResource(R.string.create_trip_reminders_body_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasIncompleteDraft) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(PacklyRadius.default)) {
                    Text(if (deadline == null && !hasIncompleteDraft) stringResource(R.string.action_choose_date) else stringResource(R.string.action_change))
                }
                OutlinedButton(enabled = deadline != null || hasIncompleteDraft, onClick = { showTimePicker = true }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(PacklyRadius.default)) {
                    Text(if (hasIncompleteDraft) stringResource(R.string.action_choose_time) else stringResource(R.string.action_change_time))
                }
            }
            TextButton(
                enabled = deadline != null || hasIncompleteDraft,
                onClick = {
                    pendingDate = null
                    onClear()
                },
                modifier = Modifier.align(Alignment.End).defaultMinSize(minHeight = 48.dp),
            ) { Text(stringResource(R.string.action_clear)) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineDateMillis(pendingDate ?: selectedDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDate = datePickerState.selectedDateMillis?.toLocalDateUtc() ?: LocalDate.now()
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.action_next_choose_time)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.create_trip_pack_by_time_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pendingDate ?: selectedDate ?: LocalDate.now()
                    onDeadlineChange(PacklyDeadlineFormatter.toInstantString(date, LocalTime.of(timePickerState.hour, timePickerState.minute)))
                    pendingDate = null
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_save_time)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ListSelectionCard(
    list: PacklyList,
    entries: List<PacklyListEntry>,
    selectedEntryIds: Set<com.dobyllm.packly.core.model.ListEntryId>,
    onToggleList: () -> Unit,
    onToggleEntry: (com.dobyllm.packly.core.model.ListEntryId) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val listName = list.displayName()
    val selectedCount = entries.count { it.id in selectedEntryIds }
    val selected = selectedCount > 0
    val toggleState = when {
        selectedCount == 0 || entries.isEmpty() -> ToggleableState.Off
        selectedCount == entries.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val selectionContentDescription = stringResource(
        if (selected) R.string.a11y_deselect_list else R.string.a11y_select_list,
        listName,
        entries.size,
    )
    Surface(
        onClick = onToggleList,
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Checkbox
            contentDescription = selectionContentDescription
        },
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(modifier = Modifier.padding(PacklySpacing.sm), verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                TriStateCheckbox(
                    state = toggleState,
                    onClick = onToggleList,
                    modifier = Modifier.semantics {
                        role = Role.Checkbox
                        contentDescription = selectionContentDescription
                    },
                )
                Column(Modifier.weight(1f)) {
                    Text(listName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val description = list.displayDescription().ifBlank { stringResource(R.string.item_count_lower, entries.size) }
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(if (expanded) R.string.a11y_collapse_list else R.string.a11y_expand_list, listName),
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                    ) {
                        Checkbox(
                            checked = entry.id in selectedEntryIds,
                            onCheckedChange = { onToggleEntry(entry.id) },
                        )
                        Text(
                            text = entry.displayItemNameSnapshot(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<com.dobyllm.packly.core.model.PacklyCategory>, selectedCategoryId: CategoryId?, onSelected: (CategoryId?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
        item {
            PacklyFilterChip(selected = selectedCategoryId == null, onClick = { onSelected(null) }, label = stringResource(R.string.filter_all_items))
        }
        items(categories, key = { it.id }) { category ->
            PacklyFilterChip(selected = selectedCategoryId == category.id, onClick = { onSelected(category.id) }, label = category.displayLabel())
        }
    }
}

@Composable
private fun ItemSelectionCard(item: PacklyItem, category: String, selected: Boolean, includedFromList: Boolean, onToggle: () -> Unit) {
    val itemName = item.displayName()
    val selectionContentDescription = stringResource(
        if (selected) R.string.a11y_selectable_item_selected else R.string.a11y_selectable_item_not_selected,
        itemName,
        category,
    )
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Checkbox
            contentDescription = selectionContentDescription
        },
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(modifier = Modifier.padding(PacklySpacing.sm), horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(itemName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (includedFromList) stringResource(R.string.already_included_suffix) else category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuantityReviewRow(name: String, category: String, quantity: Int, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(modifier = Modifier.padding(PacklySpacing.sm), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.a11y_remove_item_from_trip, name)) }
                IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) { Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.a11y_decrease_quantity, name)) }
                Text(stringResource(R.string.quantity_times, quantity), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.a11y_increase_quantity, name)) }
            }
        }
    }
}

@Composable
private fun StepFooterButtons(onBack: () -> Unit, nextLabel: String, nextEnabled: Boolean, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(PacklyRadius.default)) { Text(stringResource(R.string.action_back)) }
        Button(enabled = nextEnabled, onClick = onNext, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(PacklyRadius.default)) { Text(nextLabel) }
    }
}

@Composable
private fun DisabledReason(messageRes: Int) {
    Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyStateCard(titleRes: Int, bodyRes: Int) {
    Surface(shape = RoundedCornerShape(PacklyRadius.lg), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.fillMaxWidth().padding(PacklySpacing.marginMobile), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PacklyFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(PacklyRadius.default),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = MaterialTheme.colorScheme.surfaceContainerHigh, selectedBorderColor = MaterialTheme.colorScheme.primary),
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer),
    )
}

@Composable
private fun CreateTripTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String? = null, supportingText: String? = null, isError: Boolean = false, singleLine: Boolean = false) {
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
private fun CreateTripDiscardDialog(draftState: CreateTripDraftState, onCloseConfirmed: () -> Unit) {
    if (!draftState.showDiscardDialog) return
    AlertDialog(
        onDismissRequest = draftState::keepEditing,
        title = { Text(stringResource(R.string.create_trip_discard_title)) },
        text = { Text(stringResource(R.string.create_trip_discard_body)) },
        confirmButton = {
            TextButton(onClick = { draftState.discard(); onCloseConfirmed() }) { Text(stringResource(R.string.create_trip_discard)) }
        },
        dismissButton = { TextButton(onClick = draftState::keepEditing) { Text(stringResource(R.string.create_trip_keep_editing)) } },
    )
}

private fun deadlineDateMillis(date: LocalDate?): Long? = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private data class TripReviewItem(val itemId: ItemId, val name: String, val categoryId: CategoryId, val sortOrder: Int)

@Composable
private fun buildTripReviewItems(selectedItemIds: Set<ItemId>, items: List<PacklyItem>, sourceEntries: List<PacklyListEntry>): List<TripReviewItem> {
    val sourceReviewItems = sourceEntries.mapNotNull { entry ->
        val itemId = entry.itemId ?: return@mapNotNull null
        if (itemId !in selectedItemIds) return@mapNotNull null
        TripReviewItem(itemId, entry.displayItemNameSnapshot(), entry.categoryIdSnapshot, entry.sortOrder)
    }
    val sourceIds = sourceReviewItems.map { it.itemId }.toSet()
    val isolatedReviewItems = items
        .filter { item -> item.id in selectedItemIds && item.id !in sourceIds }
        .mapIndexed { index, item -> TripReviewItem(item.id, item.displayName(), item.categoryId, sourceEntries.size + index) }
    return (sourceReviewItems + isolatedReviewItems).distinctBy { it.itemId }.sortedBy { it.sortOrder }
}
