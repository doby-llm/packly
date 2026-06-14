@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dobyllm.packly.feature.trips.create

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.feature.items.EditItemSheet
import com.dobyllm.packly.notification.canPostPacklyNotifications
import com.dobyllm.packly.ui.component.CategoryRowsContainer
import com.dobyllm.packly.ui.component.CategorySectionCard
import com.dobyllm.packly.ui.component.ItemRowDivider
import com.dobyllm.packly.ui.component.PacklyFabAction
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
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                leadingIcon = Icons.Rounded.FlightTakeoff,
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsAvailable by remember { mutableStateOf(canPostPacklyNotifications(context)) }
    var notificationPermissionRequestPending by remember { mutableStateOf(false) }
    var notificationPermissionRequested by remember { mutableStateOf(false) }
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    // Some create-trip hosts/previews do not provide a registry; reminder editing must remain usable.
    val requestNotificationPermission = if (activityResultRegistryOwner != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionRequestPending = false
            notificationsAvailable = granted && canPostPacklyNotifications(context)
        }
    } else {
        null
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAvailable = canPostPacklyNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestNotificationPermissionIfNeeded() {
        notificationsAvailable = canPostPacklyNotifications(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsAvailable &&
            !notificationPermissionRequested
        ) {
            notificationPermissionRequested = true
            if (requestNotificationPermission != null) {
                notificationPermissionRequestPending = true
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
                onNext = {
                    if (draftState.packBy != null) requestNotificationPermissionIfNeeded()
                    onNext()
                },
            )
        },
    ) {
        item {
            PackByPickerCard(
                deadline = draftState.packBy,
                notificationsAvailable = notificationsAvailable,
                notificationPermissionRequestPending = notificationPermissionRequestPending,
                onDeadlineChange = draftState::updatePackBy,
                onIncompleteChange = draftState::updateReminderDraftIncomplete,
                onClear = draftState::clearPackBy,
                onReminderComplete = {
                    notificationsAvailable = canPostPacklyNotifications(context)
                    requestNotificationPermissionIfNeeded()
                },
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
        contentVerticalSpacing = PacklySpacing.sm,
        footer = {
            Text(
                text = activeLists.count { list -> list.entries.any { it.id in selectedEntryIds } }.let { count -> pluralStringResource(R.plurals.selected_count_label, count, count) },
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
    onFabActionChange: ((PacklyFabAction?) -> Unit)? = null,
    onAddItem: (String, CategoryId, String) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    val categories = doc.categories.filterNot { it.isArchived }.sortedBy { it.sortOrder }
    val activeItems = doc.items.filterNot { it.isArchived }
    val selectedLists = draftState.selectedSourceListIds.mapNotNull { listId -> doc.lists.firstOrNull { it.id == listId && !it.isArchived } }
    val sourceEntries = selectedLists.flatMap { list -> list.entries.sortedBy(PacklyListEntry::sortOrder).filter { it.id in draftState.selectedListEntryIds } }
    val sourceEntryByItemId = sourceEntries.mapNotNull { entry -> entry.itemId?.let { itemId -> itemId to entry } }.toMap()
    val sourceItemIds = sourceEntries.mapNotNull { it.itemId }.toSet()
    val selectedIdsForQuantity = sourceItemIds + draftState.selectedItemIds
    LaunchedEffect(selectedIdsForQuantity) { draftState.syncQuantitiesFor(selectedIdsForQuantity) }
    val filteredItems = activeItems.filter { item ->
        (selectedCategoryId == null || item.categoryId == selectedCategoryId) &&
            (query.isBlank() || item.displayName().contains(query.trim(), ignoreCase = true))
    }
    val filteredItemsByCategory = filteredItems.groupBy { it.categoryId }
    val hasVisibleItems = categories.any { category -> filteredItemsByCategory[category.id].orEmpty().isNotEmpty() }
    val canFinish = draftState.name.trim().isNotEmpty() && !draftState.duplicateNameIn(doc) && !draftState.reminderDraftIncomplete
    val addItemLabel = stringResource(R.string.action_add_item)

    DisposableEffect(onFabActionChange, addItemLabel) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = addItemLabel, onClick = { showAddItemSheet = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

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
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelected = { selectedCategoryId = it },
            )
        }
        when {
            activeItems.isEmpty() -> item { EmptyStateCard(R.string.create_trip_items_empty_title, R.string.create_trip_items_empty_body) }
            !hasVisibleItems -> item { EmptyStateCard(R.string.no_matches_title, R.string.no_matches_body) }
            else -> categories.forEach { category ->
                val sectionItems = filteredItemsByCategory[category.id].orEmpty()
                if (sectionItems.isNotEmpty()) {
                    item(key = "create_trip_category_${category.id}") {
                        val selectedCount = sectionItems.count { item -> item.id in draftState.selectedItemIds || item.id in sourceItemIds }
                        CategorySectionCard(
                            category = category,
                            countLabel = if (selectedCount > 0) {
                                pluralStringResource(R.plurals.selected_count_label, selectedCount, selectedCount)
                            } else {
                                pluralStringResource(R.plurals.item_count_lower, sectionItems.size, sectionItems.size)
                            },
                        ) {
                            CategoryRowsContainer {
                                sectionItems.forEachIndexed { index, item ->
                                    val includedFromList = item.id in sourceItemIds
                                    val selected = item.id in draftState.selectedItemIds || includedFromList
                                    ItemSelectionRow(
                                        itemName = sourceEntryByItemId[item.id]?.displayItemNameSnapshot() ?: item.displayName(),
                                        category = category.displayLabel(),
                                        selected = selected,
                                        includedFromList = includedFromList,
                                        quantity = draftState.itemQuantities[item.id] ?: 1,
                                        onToggle = { if (includedFromList) draftState.removeSourceItems(item.id, selectedLists) else draftState.toggleItem(item.id) },
                                        onQuantityChange = { draftState.setQuantity(item.id, it) },
                                    )
                                    if (index < sectionItems.lastIndex) ItemRowDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemSheet) {
        EditItemSheet(
            categories = categories,
            existingNames = activeItems.map { it.name },
            onDismiss = { showAddItemSheet = false },
            onSave = onAddItem,
        )
    }
}

@Composable
private fun CreateTripStepScaffold(
    step: Int,
    title: String,
    body: String,
    contentPadding: PaddingValues,
    contentVerticalSpacing: Dp = PacklySpacing.md,
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
                verticalArrangement = Arrangement.spacedBy(contentVerticalSpacing),
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
    notificationsAvailable: Boolean,
    notificationPermissionRequestPending: Boolean,
    onDeadlineChange: (InstantString?) -> Unit,
    onIncompleteChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onReminderComplete: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val hasIncompleteDraft = pendingDate != null
    val selectedDate = PacklyDeadlineFormatter.localDate(deadline)
    val selectedTime = PacklyDeadlineFormatter.localTime(deadline) ?: LocalTime.now().withSecond(0).withNano(0)
    val showNotificationsOffWarning = deadline != null && !notificationsAvailable && !notificationPermissionRequestPending
    val selectedDateDisplay = pendingDate?.formatCreateTripDate()
        ?: PacklyDeadlineFormatter.formatDate(deadline)
        ?: stringResource(R.string.create_trip_departure_date_placeholder)
    val selectedTimeDisplay = PacklyDeadlineFormatter.formatTime(deadline)
        ?: stringResource(R.string.create_trip_departure_time_placeholder)
    LaunchedEffect(hasIncompleteDraft) { onIncompleteChange(hasIncompleteDraft) }

    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        ReminderSelectionCard(
            icon = Icons.Rounded.CalendarMonth,
            title = stringResource(R.string.create_trip_departure_date_title),
            value = selectedDateDisplay,
            supportingText = stringResource(R.string.create_trip_reminders_body_disabled),
            actionLabel = if (deadline == null && !hasIncompleteDraft) stringResource(R.string.action_choose_date) else stringResource(R.string.action_change),
            onAction = { showDatePicker = true },
        )
        ReminderSelectionCard(
            icon = Icons.Rounded.Schedule,
            title = stringResource(R.string.create_trip_departure_time_title),
            value = selectedTimeDisplay,
            supportingText = when {
                hasIncompleteDraft -> stringResource(R.string.create_trip_pack_by_incomplete)
                deadline != null && notificationsAvailable -> stringResource(R.string.create_trip_reminders_body_enabled)
                showNotificationsOffWarning -> stringResource(R.string.deadline_notifications_off)
                else -> stringResource(R.string.create_trip_precise_reminder_helper)
            },
            isError = hasIncompleteDraft,
            actionLabel = if (hasIncompleteDraft) stringResource(R.string.action_choose_time) else stringResource(R.string.action_change_time),
            actionEnabled = deadline != null || hasIncompleteDraft,
            onAction = { showTimePicker = true },
        )
        TextButton(
            enabled = deadline != null || hasIncompleteDraft,
            onClick = {
                pendingDate = null
                onClear()
            },
            modifier = Modifier.align(Alignment.End).defaultMinSize(minHeight = 48.dp),
        ) { Text(stringResource(R.string.action_clear)) }
        // Permission is requested automatically after date+time selection when a registry is available.
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
            title = { Text(stringResource(R.string.create_trip_departure_time_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pendingDate ?: selectedDate ?: LocalDate.now()
                    onDeadlineChange(PacklyDeadlineFormatter.toInstantString(date, LocalTime.of(timePickerState.hour, timePickerState.minute)))
                    pendingDate = null
                    showTimePicker = false
                    onReminderComplete()
                }) { Text(stringResource(R.string.action_save_time)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ReminderSelectionCard(
    icon: ImageVector,
    title: String,
    value: String,
    supportingText: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(PacklySpacing.marginMobile),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(PacklyRadius.full)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                enabled = actionEnabled,
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) { Text(actionLabel) }
        }
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
    val selectionContentDescription = pluralStringResource(
        if (selected) R.plurals.a11y_deselect_list else R.plurals.a11y_select_list,
        entries.size,
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
                    val description = list.displayDescription().ifBlank { pluralStringResource(R.plurals.item_count_lower, entries.size, entries.size) }
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
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(vertical = PacklySpacing.xs),
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
private fun ItemSelectionRow(
    itemName: String,
    category: String,
    selected: Boolean,
    includedFromList: Boolean,
    quantity: Int,
    onToggle: () -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
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
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(itemName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (includedFromList) stringResource(R.string.already_included_suffix) else category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                InlineQuantityStepper(
                    itemName = itemName,
                    quantity = quantity,
                    onQuantityChange = onQuantityChange,
                )
            }
        }
    }
}

@Composable
private fun InlineQuantityStepper(itemName: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
        ) {
            IconButton(
                onClick = { onQuantityChange(quantity - 1) },
                enabled = quantity > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.a11y_decrease_quantity, itemName))
            }
            Text(
                text = "$quantity",
                modifier = Modifier.widthIn(min = 24.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = { onQuantityChange(quantity + 1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.a11y_increase_quantity, itemName))
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
private fun CreateTripTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String? = null, supportingText: String? = null, isError: Boolean = false, singleLine: Boolean = false, leadingIcon: ImageVector? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
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
private fun LocalDate.formatCreateTripDate(): String = format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
