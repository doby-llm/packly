package com.dobyllm.packly.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun PacklyProgress(
    tripTitle: String,
    packed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val safeTotal = total.coerceAtLeast(1)
    val rawProgress = packed / safeTotal.toFloat()
    val progress = rawProgress.coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()
    val progressDescription = stringResource(R.string.a11y_packing_progress, percent, packed, total)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "packing-progress",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        Text(
            text = tripTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.packing_progress_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(PacklyRadius.full))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .semantics {
                        contentDescription = progressDescription
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(PacklyRadius.full))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
            }
            Text(
                text = pluralStringResource(R.plurals.packed_fraction, total, packed, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
