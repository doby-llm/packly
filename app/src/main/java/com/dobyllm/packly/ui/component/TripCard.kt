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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val deadlineWarning = summary.unpackedItems > 0 && PacklyDeadlineFormatter.isCloseOrOverdue(trip.packBy)
    val archiveDescription = stringResource(R.string.a11y_archive_trip, trip.name)

    TripSummaryCard(
        title = trip.name,
        metadata = summary.metadata,
        percentLabel = "${summary.progressPercent}% Packed",
        progress = summary.progress,
        chips = summary.chips,
        accentColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = null,
    ) {
        if (deadlineWarning) {
            AssistChip(
                onClick = onOpen,
                label = { Text("Pack-by reminder due soon with unpacked items") },
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
            ) { Text(if (summary.packedItems == 0) "Start packing" else "Continue packing") }
            OutlinedButton(
                onClick = onOpen,
                contentPadding = CompactTripButtonPadding,
                modifier = Modifier.semantics {
                    contentDescription = "Modify ${trip.name}"
                    role = Role.Button
                },
            ) { Text("Modify") }
            TextButton(
                onClick = onDelete,
                contentPadding = CompactTripButtonPadding,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.semantics {
                    contentDescription = archiveDescription
                    role = Role.Button
                },
            ) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Spacer(Modifier.width(PacklySpacing.xs))
                Text(stringResource(R.string.action_archive))
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
    val progress: Float,
    val progressPercent: Int,
    val metadata: String,
    val chips: List<String>,
)

private fun computeTripSummary(trip: PacklyTrip): TripSummary {
    val packed = trip.entries.count { it.isPacked }
    val total = trip.entries.size
    val safeTotal = total.coerceAtLeast(1)
    val progress = packed / safeTotal.toFloat()
    val packBy = PacklyDeadlineFormatter.formatDisplay(trip.packBy)
    val dateRange = listOfNotNull(trip.startDate, trip.endDate).joinToString(" - ").ifBlank { null }
    val metadata = dateRange
        ?: packBy?.let { "Pack by $it" }
        ?: trip.destination.takeIf { it.isNotBlank() }
        ?: "$packed/$total packed"
    val chips = listOfNotNull(
        trip.destination.takeIf { it.isNotBlank() },
        packBy?.let { "Pack by $it" },
        total.takeIf { it > 0 }?.let { "$it items" },
    )

    return TripSummary(
        packedItems = packed,
        unpackedItems = total - packed,
        progress = progress,
        progressPercent = (progress * 100).roundToInt(),
        metadata = metadata,
        chips = chips,
    )
}

private const val MAX_VISIBLE_TRIP_CHIPS = 2
private val CompactTripButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
