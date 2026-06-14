@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.time.PacklyDeadlineFormatter
import com.dobyllm.packly.ui.token.PacklyElevation
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import kotlin.math.roundToInt

@Composable
fun TripCard(trip: PacklyTrip, onOpen: () -> Unit, onPack: () -> Unit, onDelete: () -> Unit) {
    val summary = computeTripSummary(trip)
    val packByLabel = summary.packBy?.let { stringResource(R.string.deadline_pack_by, it) }
    val fallbackPackedLabel = pluralStringResource(R.plurals.packed_fraction, summary.totalItems, summary.packedItems, summary.totalItems)
    val itemCountLabel = pluralStringResource(R.plurals.item_count_lower, summary.totalItems, summary.totalItems)
    val deadlineWarning = summary.unpackedItems > 0 && PacklyDeadlineFormatter.isCloseOrOverdue(trip.packBy)
    val archiveDescription = stringResource(R.string.a11y_archive_trip, trip.name)
    val modifyDescription = stringResource(R.string.a11y_modify_trip, trip.name)

    TripSummaryCard(
        title = trip.name,
        metadata = summary.metadata(fallbackPackedLabel),
        percentLabel = stringResource(R.string.percent_packed, summary.progressPercent),
        progress = summary.progress,
        chips = summary.chips(packByLabel, itemCountLabel),
        accentColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = null,
    ) {
        if (deadlineWarning) {
            AssistChip(
                onClick = onPack,
                label = { Text(stringResource(R.string.trip_deadline_due_soon)) },
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
        ) {
            Button(
                onClick = onPack,
                contentPadding = CompactTripButtonPadding,
            ) { Text(if (summary.packedItems == 0) stringResource(R.string.action_start_packing) else stringResource(R.string.action_continue_packing)) }
            OutlinedButton(
                onClick = onOpen,
                contentPadding = CompactTripButtonPadding,
                modifier = Modifier.semantics {
                    contentDescription = modifyDescription
                    role = Role.Button
                },
            ) { Text(stringResource(R.string.action_modify)) }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = archiveDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun TripSummaryCard(
    title: String,
    metadata: String,
    percentLabel: String,
    progress: Float,
    chips: List<String>,
    accentColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val cardShape = RoundedCornerShape(PacklyRadius.lg)

    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = PacklyElevation.card,
            shape = cardShape,
            ambientColor = accentColor.copy(alpha = 0.10f),
            spotColor = accentColor.copy(alpha = 0.10f),
        )
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)

    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = PacklyElevation.floor),
        ) {
            TripSummaryCardContent(title, metadata, percentLabel, progress, chips, accentColor, actions)
        }
    } else {
        Surface(
            modifier = cardModifier,
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = PacklyElevation.floor,
        ) {
            TripSummaryCardContent(title, metadata, percentLabel, progress, chips, accentColor, actions)
        }
    }
}

@Composable
private fun TripSummaryCardContent(
    title: String,
    metadata: String,
    percentLabel: String,
    progress: Float,
    chips: List<String>,
    accentColor: Color,
    actions: @Composable ColumnScope.() -> Unit,
) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (metadata.isNotBlank()) {
                            Text(
                                metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.semantics { contentDescription = metadata },
                            )
                        }
                    }
                    ProgressPill(percentLabel)
                }
                PacklyProgressBar(progress = progress)
                if (chips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                        verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                    ) {
                        chips.take(MAX_VISIBLE_TRIP_CHIPS).forEachIndexed { index, chip ->
                            TripMetadataChip(
                                label = chip,
                                useTertiaryAccent = index % 2 == 1,
                            )
                        }
                    }
                }
                actions()
            }
        }
}

@Composable
private fun PacklyProgressBar(progress: Float) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(PacklyRadius.full))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ProgressPill(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(PacklyRadius.full))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f))
            .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
    )
}

@Composable
private fun TripMetadataChip(label: String, useTertiaryAccent: Boolean) {
    val background = if (useTertiaryAccent) {
        MaterialTheme.colorScheme.tertiaryFixed.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.secondaryFixed.copy(alpha = 0.72f)
    }
    val foreground = if (useTertiaryAccent) {
        MaterialTheme.colorScheme.onTertiaryFixedVariant
    } else {
        MaterialTheme.colorScheme.onSecondaryFixedVariant
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(PacklyRadius.full))
            .background(background)
            .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.xs),
    )
}

private data class TripSummary(
    val packedItems: Int,
    val unpackedItems: Int,
    val totalItems: Int,
    val progress: Float,
    val progressPercent: Int,
    val dateRange: String?,
    val packBy: String?,
    val destination: String,
) {
    fun metadata(fallbackPackedLabel: String): String = listOfNotNull(
        dateRange,
        destination.takeIf { it.isNotBlank() },
        fallbackPackedLabel,
    ).joinToString(" • ")

    fun chips(packByLabel: String?, itemCountLabel: String): List<String> = listOfNotNull(
        destination.takeIf { it.isNotBlank() },
        packByLabel,
        totalItems.takeIf { it > 0 }?.let { itemCountLabel },
    )
}

private fun computeTripSummary(trip: PacklyTrip): TripSummary {
    val packed = trip.entries.count { it.isPacked }
    val total = trip.entries.size
    val safeTotal = total.coerceAtLeast(1)
    val progress = packed / safeTotal.toFloat()
    val packBy = PacklyDeadlineFormatter.formatDisplay(trip.packBy)
    val dateRange = listOfNotNull(trip.startDate, trip.endDate).joinToString(" - ").ifBlank { null }
    return TripSummary(
        packedItems = packed,
        unpackedItems = total - packed,
        totalItems = total,
        progress = progress,
        progressPercent = (progress * 100).roundToInt(),
        dateRange = dateRange,
        packBy = packBy,
        destination = trip.destination,
    )
}

private const val MAX_VISIBLE_TRIP_CHIPS = 2
private val CompactTripButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
