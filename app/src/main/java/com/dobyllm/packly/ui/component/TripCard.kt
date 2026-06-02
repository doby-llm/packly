package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    TripSummaryCard(
        title = trip.name,
        metadata = summary.metadata,
        percentLabel = "${summary.progressPercent}% Packed",
        progress = summary.progress,
        chips = summary.chips,
        accentColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onOpen,
    ) {
        if (deadlineWarning) {
            AssistChip(
                onClick = onOpen,
                label = { Text("Pack-by reminder due soon with unpacked items") },
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
            Button(onClick = onPack) { Text(if (summary.packedItems == 0) "Start packing" else "Continue packing") }
            OutlinedButton(onClick = onOpen) { Text("Details") }
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val cardShape = RoundedCornerShape(PacklyRadius.lg)

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = PacklyElevation.card,
                shape = cardShape,
                ambientColor = accentColor.copy(alpha = 0.10f),
                spotColor = accentColor.copy(alpha = 0.10f),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = PacklyElevation.floor),
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
                    .padding(PacklySpacing.md),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
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
                        Text(title, style = MaterialTheme.typography.titleLarge)
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
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(PacklyRadius.full)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                if (chips.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
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
    val metadata = listOfNotNull(
        dateRange,
        trip.destination.takeIf { it.isNotBlank() },
        "$packed/$total packed",
        packBy?.let { "Pack by $it" },
    ).joinToString(" • ")
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
