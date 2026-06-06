@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.model.TripEntry
import com.dobyllm.packly.core.model.TripEntryId
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.theme.PacklyOnSecondaryContainer
import com.dobyllm.packly.ui.theme.PacklyOutlineVariant
import com.dobyllm.packly.ui.theme.PacklyPrimary
import com.dobyllm.packly.ui.theme.PacklySecondary
import com.dobyllm.packly.ui.theme.PacklySecondaryContainer
import com.dobyllm.packly.ui.theme.PacklySurfaceContainer
import com.dobyllm.packly.ui.theme.PacklySurfaceContainerHigh
import com.dobyllm.packly.ui.theme.PacklySurfaceContainerLow
import com.dobyllm.packly.ui.theme.PacklySurfaceContainerLowest
import com.dobyllm.packly.ui.token.CategoryTokens
import com.dobyllm.packly.ui.token.PacklyElevation
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import kotlin.math.roundToInt

private enum class ModifyTripTab { CurrentPlan, Browse }

@Composable
fun TripDetailScreen(
    doc: PacklyAppDocument,
    tripId: TripId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPack: () -> Unit,
    onReset: () -> Unit,
    onQuantityChange: (TripEntryId, Int) -> Unit,
    onRemoveEntry: (TripEntryId) -> Unit,
    onDeadlineChange: (InstantString?) -> Unit,
    onToggleSourceList: (ListId) -> Unit,
    onToggleSourceItem: (ItemId) -> Unit,
) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var activeTab by rememberSaveable(tripId) { mutableStateOf(ModifyTripTab.CurrentPlan) }
    var showResetConfirm by remember(tripId) { mutableStateOf(false) }
    var deadlineDraft by remember(trip?.packBy) { mutableStateOf(trip?.packBy) }
    var notificationPermissionGranted by rememberNotificationPermissionState()
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    val hasDeadlineChange = trip != null && deadlineDraft != trip.packBy

    if (trip == null) {
        EmptyState(
            title = stringResource(R.string.trip_not_found_title),
            body = stringResource(R.string.trip_not_found_body),
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = PacklySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
        ) {
            item {
                ModifyTripHeader(
                    tripName = trip.name,
                    dateRange = dateRange,
                    activeTab = activeTab,
                    onTabChange = { activeTab = it },
                )
            }

            when (activeTab) {
                ModifyTripTab.CurrentPlan -> {
                    item {
                        PackingProgressCard(
                            packedItems = packedItems,
                            totalItems = totalItems,
                            progress = progress,
                            onPack = onPack,
                            onReset = { showResetConfirm = true },
                        )
                    }
                    item {
                        DeadlineSection(
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
                            modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile),
                        )
                    }
                    item {
                        CurrentPlanSection(
                            doc = doc,
                            entries = trip.entries,
                            onQuantityChange = onQuantityChange,
                            onRemoveEntry = onRemoveEntry,
                            onBrowseClick = { activeTab = ModifyTripTab.Browse },
                        )
                    }
                }

                ModifyTripTab.Browse -> {
                    item {
                        BrowseTripContent(
                            doc = doc,
                            tripEntries = trip.entries,
                            onToggleSourceList = onToggleSourceList,
                            onToggleSourceItem = onToggleSourceItem,
                            modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile),
                        )
                    }
                }
            }
        }
    }

    if (trip != null && showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_packed_items_title)) },
            text = { Text(stringResource(R.string.reset_packed_items_body, trip.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_reset)) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ModifyTripHeader(
    tripName: String,
    dateRange: String?,
    activeTab: ModifyTripTab,
    onTabChange: (ModifyTripTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = PacklyRadius.xl, bottomEnd = PacklyRadius.xl),
        color = PacklySurfaceContainerLowest,
        shadowElevation = PacklyElevation.card,
    ) {
        Column(
            modifier = Modifier.padding(start = PacklySpacing.marginMobile, end = PacklySpacing.marginMobile, top = PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Text(
                text = tripName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                Text(
                    text = dateRange ?: stringResource(R.string.dates_not_set),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            ModifyTripTabs(activeTab = activeTab, onTabChange = onTabChange)
        }
    }
}

@Composable
private fun ModifyTripTabs(
    activeTab: ModifyTripTab,
    onTabChange: (ModifyTripTab) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        ModifyTripTabButton(
            label = stringResource(R.string.trip_tab_current_plan),
            selected = activeTab == ModifyTripTab.CurrentPlan,
            onClick = { onTabChange(ModifyTripTab.CurrentPlan) },
            modifier = Modifier.weight(1f),
        )
        ModifyTripTabButton(
            label = stringResource(R.string.trip_tab_browse),
            selected = activeTab == ModifyTripTab.Browse,
            onClick = { onTabChange(ModifyTripTab.Browse) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModifyTripTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Tab
            this.selected = selected
        },
        color = if (selected) PacklySurfaceContainerLow.copy(alpha = 0.5f) else Color.Transparent,
        contentColor = if (selected) PacklyPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                modifier = Modifier.padding(vertical = PacklySpacing.sm),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (selected) PacklyPrimary else Color.Transparent,
                content = {},
            )
        }
    }
}

@Composable
private fun PackingProgressCard(
    packedItems: Int,
    totalItems: Int,
    progress: Float,
    onPack: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile).fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = PacklySurfaceContainerLowest,
        border = BorderStroke(1.dp, PacklyOutlineVariant),
        shadowElevation = PacklyElevation.card,
    ) {
        Column(
            modifier = Modifier.padding(PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.packing_progress_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(R.string.percent_packed, (progress * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = PacklyPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(R.string.items_packed_count, packedItems, totalItems), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Stacked full-width actions keep equal 48dp height on 360dp phones and prevent Reset copy wrapping.
            Button(
                onClick = onPack,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) {
                Text(if (packedItems == 0) stringResource(R.string.action_start_packing) else stringResource(R.string.action_continue_packing), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) {
                Text(stringResource(R.string.action_reset_packed_items), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DeadlineSection(
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        Text(stringResource(R.string.trip_settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        packBy?.let { displayValue ->
            Text(
                if (deadlineWarning) {
                    stringResource(R.string.deadline_reminder_warning, displayValue, unpackedItems)
                } else {
                    stringResource(R.string.deadline_pack_by, displayValue)
                },
                color = if (deadlineWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PacklyDeadlinePickerField(
            deadline = deadlineDraft,
            onDeadlineChange = onDeadlineDraftChange,
            label = stringResource(R.string.deadline_pack_by_label),
            supportingText = when {
                deadlineDraft != null && !notificationPermissionGranted -> stringResource(R.string.deadline_notifications_off)
                else -> stringResource(R.string.deadline_supporting_text)
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            Button(
                enabled = hasDeadlineChange,
                onClick = onSaveDeadline,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) { Text(stringResource(R.string.action_save_pack_by), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            OutlinedButton(
                enabled = canClearDeadline,
                onClick = onClearDeadline,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) { Text(stringResource(R.string.action_clear), maxLines = 1, softWrap = false) }
        }
    }
}

@Composable
private fun CurrentPlanSection(
    doc: PacklyAppDocument,
    entries: List<TripEntry>,
    onQuantityChange: (TripEntryId, Int) -> Unit,
    onRemoveEntry: (TripEntryId) -> Unit,
    onBrowseClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.current_plan_items_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            CountChip(stringResource(R.string.items_uppercase_count, entries.size))
        }

        if (entries.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_trip_items_title),
                body = stringResource(R.string.empty_trip_items_body),
            )
            Button(
                onClick = onBrowseClick,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
            ) { Text(stringResource(R.string.action_browse_items)) }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PacklyRadius.lg),
                color = PacklySurfaceContainerLowest,
                border = BorderStroke(1.dp, PacklySurfaceContainerHigh),
                shadowElevation = PacklyElevation.card,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    entries.forEachIndexed { index, entry ->
                        CurrentPlanEntryRow(
                            entry = entry,
                            sourceLabel = entry.sourceLabel(doc),
                            onQuantityChange = { onQuantityChange(entry.id, it) },
                            onRemoveEntry = { onRemoveEntry(entry.id) },
                        )
                        if (index != entries.lastIndex) {
                            Surface(Modifier.fillMaxWidth().height(1.dp), color = PacklySurfaceContainerHigh, content = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPlanEntryRow(
    entry: TripEntry,
    sourceLabel: TripSourceLabel,
    onQuantityChange: (Int) -> Unit,
    onRemoveEntry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
            Text(entry.nameSnapshot, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = sourceLabel.displayText(),
                style = MaterialTheme.typography.labelMedium,
                color = if (sourceLabel is TripSourceLabel.FromList) PacklySecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = if (sourceLabel is TripSourceLabel.IndividualItem) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        QuantityStepper(quantity = entry.quantity, itemName = entry.nameSnapshot, onQuantityChange = onQuantityChange)
        IconButton(onClick = onRemoveEntry, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.a11y_remove_named, entry.nameSnapshot), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    itemName: String,
    onQuantityChange: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = PacklySurfaceContainerLow,
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
            Text("$quantity", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
private fun BrowseTripContent(
    doc: PacklyAppDocument,
    tripEntries: List<TripEntry>,
    onToggleSourceList: (ListId) -> Unit,
    onToggleSourceItem: (ItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeLists = doc.lists.filterNot { it.isArchived }
    val activeItems = doc.items.filterNot { it.isArchived }
    val categories = doc.categories.filterNot { it.isArchived }.sortedBy { it.sortOrder }
    val fallbackCategoryLabel = stringResource(R.string.category_miscellaneous)
    val existingKeys = remember(tripEntries) { tripEntries.map { it.dedupeKey() }.toSet() }
    val normalizedQuery = query.trim()
    val filteredItems = activeItems
        .filter { item -> selectedCategoryId == null || item.categoryId == selectedCategoryId }
        .filter { item -> normalizedQuery.isBlank() || item.name.contains(normalizedQuery, ignoreCase = true) || doc.categoryFor(item.categoryId, fallbackCategoryLabel).label.contains(normalizedQuery, ignoreCase = true) }
    val filteredLists = activeLists.filter { list ->
        val matchesQuery = normalizedQuery.isBlank() || list.name.contains(normalizedQuery, ignoreCase = true) || list.description.contains(normalizedQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryId == null || list.entries.any { it.categoryIdSnapshot == selectedCategoryId }
        matchesQuery && matchesCategory
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PacklySpacing.md)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            placeholder = { Text(stringResource(R.string.search_templates_items)) },
            singleLine = true,
            shape = RoundedCornerShape(PacklyRadius.lg),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PacklySurfaceContainerLowest,
                unfocusedContainerColor = PacklySurfaceContainerLowest,
                focusedBorderColor = PacklyPrimary,
                unfocusedBorderColor = PacklyOutlineVariant,
            ),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            BrowseCategoryChip(label = stringResource(R.string.browse_category_all), selected = selectedCategoryId == null, onClick = { selectedCategoryId = null })
            categories.forEach { category ->
                BrowseCategoryChip(
                    label = category.label,
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id },
                )
            }
        }
        if (filteredLists.isEmpty() && filteredItems.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_matches_title),
                body = stringResource(R.string.no_matches_body),
            )
        } else {
            if (filteredLists.isNotEmpty()) {
                BrowseSectionHeader(title = stringResource(R.string.browse_lists_title))
                filteredLists.chunked(2).forEach { rowLists ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
                        rowLists.forEach { list ->
                            val fullyAdded = list.entries.isNotEmpty() && list.entries.all { it.dedupeKey() in existingKeys }
                            BrowseListCard(
                                list = list,
                                primaryCategory = list.entries.firstOrNull()?.let { doc.categoryFor(it.categoryIdSnapshot, fallbackCategoryLabel) },
                                alreadyAdded = fullyAdded,
                                onToggle = { onToggleSourceList(list.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowLists.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (filteredItems.isNotEmpty()) {
                BrowseSectionHeader(title = stringResource(R.string.browse_items_title))
                Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                    filteredItems.forEach { item ->
                        val category = doc.categoryFor(item.categoryId, fallbackCategoryLabel)
                        val alreadyAdded = item.dedupeKey() in existingKeys
                        BrowseItemRow(
                            item = item,
                            category = category,
                            alreadyAdded = alreadyAdded,
                            onToggle = { onToggleSourceItem(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseSectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BrowseCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(PacklyRadius.full),
        color = if (selected) PacklySecondaryContainer.copy(alpha = 0.2f) else PacklySurfaceContainer,
        border = if (selected) BorderStroke(1.dp, PacklySecondaryContainer) else null,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
            role = Role.Tab
            this.selected = selected
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = PacklySpacing.md, vertical = PacklySpacing.sm),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) PacklyOnSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BrowseListCard(
    list: PacklyList,
    primaryCategory: PacklyCategory?,
    alreadyAdded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inTripDescription = stringResource(R.string.a11y_in_trip)
    val notInTripDescription = stringResource(R.string.a11y_not_in_trip)
    val removeListDescription = stringResource(R.string.a11y_remove_list_from_trip, list.name)
    val addListDescription = stringResource(R.string.a11y_add_list_to_trip, list.name)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 164.dp)
            .shadow(12.dp, RoundedCornerShape(PacklyRadius.xl), ambientColor = PacklySecondary.copy(alpha = 0.08f), spotColor = PacklySecondary.copy(alpha = 0.08f))
            .semantics {
                stateDescription = if (alreadyAdded) inTripDescription else notInTripDescription
                contentDescription = if (alreadyAdded) removeListDescription else addListDescription
            },
        onClick = onToggle,
        shape = RoundedCornerShape(PacklyRadius.xl),
        color = PacklySurfaceContainerLowest,
    ) {
        Box(Modifier.fillMaxWidth().padding(PacklySpacing.sm)) {
            Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                val token = primaryCategory?.let { CategoryTokens.byKey(it.key) } ?: CategoryTokens.byKey("misc")
                Surface(shape = CircleShape, color = token.soft.copy(alpha = 0.25f), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(CategoryTokens.icon(token.iconKey), contentDescription = null, tint = token.accent, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(PacklySpacing.base))
                Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.item_count_lower, list.entries.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
            AddStateButton(
                alreadyAdded = alreadyAdded,
                contentDescription = if (alreadyAdded) stringResource(R.string.a11y_remove_named, list.name) else stringResource(R.string.a11y_add_named, list.name),
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun BrowseItemRow(
    item: PacklyItem,
    category: PacklyCategory,
    alreadyAdded: Boolean,
    onToggle: () -> Unit,
) {
    val inTripDescription = stringResource(R.string.a11y_in_trip)
    val notInTripDescription = stringResource(R.string.a11y_not_in_trip)
    val removeItemFromTripDescription = stringResource(R.string.a11y_remove_item_from_trip, item.name)
    val addItemDescription = stringResource(R.string.a11y_add_named, item.name)
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 92.dp)
            .semantics {
                stateDescription = if (alreadyAdded) inTripDescription else notInTripDescription
                contentDescription = if (alreadyAdded) removeItemFromTripDescription else addItemDescription
            },
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = PacklySurfaceContainerLowest,
        border = BorderStroke(1.dp, if (alreadyAdded) PacklyPrimary.copy(alpha = 0.35f) else PacklySurfaceContainerHigh),
        shadowElevation = PacklyElevation.card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(PacklySpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val token = CategoryTokens.byKey(category.key)
            Surface(shape = RoundedCornerShape(PacklyRadius.default), color = PacklySurfaceContainer, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(CategoryTokens.icon(category.iconKey), contentDescription = null, tint = token.accent, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(shape = RoundedCornerShape(PacklyRadius.full), color = PacklySurfaceContainer) {
                    Text(
                        text = category.label,
                        modifier = Modifier.padding(horizontal = PacklySpacing.base, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
            AddStateButton(
                alreadyAdded = alreadyAdded,
                contentDescription = if (alreadyAdded) stringResource(R.string.a11y_remove_named, item.name) else addItemDescription,
            )
        }
    }
}

@Composable
private fun AddStateButton(alreadyAdded: Boolean, contentDescription: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = if (alreadyAdded) PacklyPrimary else PacklySurfaceContainerHigh,
        contentColor = if (alreadyAdded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (alreadyAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CountChip(label: String) {
    Surface(shape = RoundedCornerShape(PacklyRadius.full), color = PacklySurfaceContainerHigh) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TripSourceLabel.displayText(): String = when (this) {
    is TripSourceLabel.FromList -> stringResource(R.string.source_from_list, listName)
    TripSourceLabel.IndividualItem -> stringResource(R.string.source_individual_item)
}

private sealed interface TripSourceLabel {
    data class FromList(val listName: String) : TripSourceLabel
    object IndividualItem : TripSourceLabel
}

private fun TripEntry.sourceLabel(doc: PacklyAppDocument): TripSourceLabel {
    val sourceList = sourceListEntryId?.let { entryId -> doc.lists.firstOrNull { list -> list.entries.any { it.id == entryId } } }
    return sourceList?.name?.let { TripSourceLabel.FromList(it) } ?: TripSourceLabel.IndividualItem
}

private fun PacklyAppDocument.categoryFor(categoryId: String, fallbackLabel: String): PacklyCategory =
    categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull() ?: PacklyCategory(
        id = categoryId,
        key = "misc",
        label = fallbackLabel,
        iconKey = "category",
        accentColorHex = "#3b4a44",
        softColorHex = "#e1e3e4",
        sortOrder = Int.MAX_VALUE,
    )

private fun PacklyListEntry.dedupeKey(): String = itemId?.let { "item:$it" } ?: snapshotDedupeKey(itemNameSnapshot, categoryIdSnapshot)

private fun TripEntry.dedupeKey(): String =
    sourceItemId?.let { "item:$it" } ?: snapshotDedupeKey(nameSnapshot, categoryIdSnapshot)

private fun PacklyItem.dedupeKey(): String = "item:$id"

private fun snapshotDedupeKey(name: String, categoryId: String): String =
    "snapshot:${name.trim().lowercase().replace(Regex("\\s+"), " ")}\u0000$categoryId"
