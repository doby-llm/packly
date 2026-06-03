@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dobyllm.packly.feature.packing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.TripEntry
import com.dobyllm.packly.core.model.TripEntryId
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.PackingItemRow
import com.dobyllm.packly.ui.component.PacklyProgress
import com.dobyllm.packly.ui.token.CategoryTokens
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import kotlinx.coroutines.launch

private enum class PackingFilter { All, Unpacked, Packed }

@Composable
fun PackingModeScreen(
    doc: PacklyAppDocument,
    tripId: TripId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSetPacked: (TripEntryId, Boolean) -> Unit,
) {
    val trip = doc.trips.firstOrNull { it.id == tripId }
    var filter by remember { mutableStateOf(PackingFilter.All) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (trip == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(PacklySpacing.marginMobile),
        ) {
            EmptyState(
                title = "This trip could not be found",
                body = "Back to Trips and choose an active trip to continue packing.",
            )
        }
        return
    }

    val entries = trip.entries.sortedBy { it.sortOrder }
    val packedCount = entries.count { it.isPacked }
    val unpackedCount = entries.size - packedCount
    val visible = entries.filter {
        when (filter) {
            PackingFilter.All -> true
            PackingFilter.Unpacked -> !it.isPacked
            PackingFilter.Packed -> it.isPacked
        }
    }
    val categoriesById = doc.categories.associateBy { it.id }
    val groupedEntries = visible
        .groupBy { it.categoryIdSnapshot }
        .toList()
        .sortedBy { (categoryId, _) -> categoriesById[categoryId]?.sortOrder ?: Int.MAX_VALUE }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = PacklySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
        ) {
            stickyHeader(key = "packing_progress_header") {
                PackingProgressHeader(
                    tripTitle = trip.name,
                    packedCount = packedCount,
                    totalCount = entries.size,
                    unpackedCount = unpackedCount,
                    currentFilter = filter,
                    onFilterChange = { filter = it },
                )
            }

            if (entries.isEmpty()) {
                item(key = "packing_empty") {
                    Box(Modifier.padding(horizontal = PacklySpacing.marginMobile)) {
                        EmptyState(
                            title = "Nothing to pack yet",
                            body = "Add items to this trip before entering packing mode.",
                        )
                    }
                }
            }

            groupedEntries.forEach { (categoryId, sectionEntries) ->
                val category = categoriesById[categoryId]
                val sectionKey = category?.key ?: categoryId
                item(key = "packing_section_header_$sectionKey") {
                    PackingCategoryHeader(
                        category = category,
                        packed = sectionEntries.count { it.isPacked },
                        total = sectionEntries.size,
                        modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile),
                    )
                }
                item(key = "packing_section_card_$sectionKey") {
                    PackingRowsCard(
                        entries = sectionEntries,
                        category = category,
                        onToggle = { entry ->
                            val previousState = entry.isPacked
                            val nextState = !previousState
                            onSetPacked(entry.id, nextState)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "${if (nextState) "Packed" else "Unpacked"} \"${entry.nameSnapshot}\"",
                                    actionLabel = "UNDO",
                                    withDismissAction = false,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onSetPacked(entry.id, previousState)
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile),
                    )
                }
            }

            if (visible.isEmpty() && entries.isNotEmpty()) {
                item(key = "packing_filtered_empty") {
                    Box(Modifier.padding(horizontal = PacklySpacing.marginMobile)) {
                        EmptyState(
                            title = "No ${filter.label.lowercase()} items",
                            body = "Switch filters to review the rest of your packing list.",
                        )
                    }
                }
            }

            if (entries.isNotEmpty() && entries.all { it.isPacked }) {
                item(key = "packing_complete") {
                    AllPackedPanel(Modifier.padding(horizontal = PacklySpacing.marginMobile))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.marginMobile),
        )
    }
}

@Composable
private fun PackingProgressHeader(
    tripTitle: String,
    packedCount: Int,
    totalCount: Int,
    unpackedCount: Int,
    currentFilter: PackingFilter,
    onFilterChange: (PackingFilter) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(
                start = PacklySpacing.marginMobile,
                top = PacklySpacing.md,
                end = PacklySpacing.marginMobile,
                bottom = PacklySpacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
        ) {
            PacklyProgress(
                tripTitle = tripTitle,
                packed = packedCount,
                total = totalCount,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
            ) {
                PackingFilterChip("All Items", currentFilter == PackingFilter.All) { onFilterChange(PackingFilter.All) }
                PackingFilterChip("Unpacked ($unpackedCount)", currentFilter == PackingFilter.Unpacked) { onFilterChange(PackingFilter.Unpacked) }
                PackingFilterChip("Packed ($packedCount)", currentFilter == PackingFilter.Packed) { onFilterChange(PackingFilter.Packed) }
            }
        }
    }
}

@Composable
private fun PackingFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(PacklyRadius.full),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (selected) 0f else 1f)),
        modifier = Modifier.semantics { contentDescription = "$label filter${if (selected) ", selected" else ""}" },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.base),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PackingCategoryHeader(
    category: PacklyCategory?,
    packed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val token = CategoryTokens.byKey(category?.key ?: "misc")
    Column(
        modifier = modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
        ) {
            Icon(
                imageVector = CategoryTokens.icon(category?.iconKey ?: "category"),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = category?.label ?: "Other",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$packed/$total",
                style = MaterialTheme.typography.labelMedium,
                color = token.accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

@Composable
private fun PackingRowsCard(
    entries: List<TripEntry>,
    category: PacklyCategory?,
    onToggle: (TripEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.default),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 2.dp,
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                PackingItemRow(
                    entry = entry,
                    category = category,
                    onToggle = { onToggle(entry) },
                )
                if (index < entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(start = 64.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllPackedPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(PacklySpacing.marginMobile),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column {
                Text(
                    text = "All Packed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Everything on this trip is ready to go.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private val PackingFilter.label: String
    get() = when (this) {
        PackingFilter.All -> "All"
        PackingFilter.Unpacked -> "Unpacked"
        PackingFilter.Packed -> "Packed"
    }
