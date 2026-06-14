package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.TripEntry
import com.dobyllm.packly.ui.i18n.displayLabel
import com.dobyllm.packly.ui.i18n.displayName
import com.dobyllm.packly.ui.i18n.displayNameSnapshot
import com.dobyllm.packly.ui.token.CategoryTokens
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ItemRow(
    item: PacklyItem,
    category: PacklyCategory?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    hasDuplicate: Boolean = false,
) {
    val displayName = item.displayName()
    CatalogItemRow(
        name = displayName,
        note = item.notes,
        category = category,
        modifier = modifier,
        minHeight = 48.dp,
        verticalPadding = PacklySpacing.xs,
        onClick = onEdit,
        trailingContent = {
            if (hasDuplicate) DuplicateWarningIcon()
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.a11y_archive_item, displayName),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
fun PackingItemRow(
    entry: TripEntry,
    category: PacklyCategory?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val note = entry.notes.trim()
    val displayName = entry.displayNameSnapshot()
    val categoryLabel = category?.displayLabel()
    val unknownCategory = stringResource(R.string.a11y_unknown_category)
    val quantityDescription = stringResource(R.string.a11y_quantity, entry.quantity)
    val packed = stringResource(R.string.a11y_packed)
    val unpacked = stringResource(R.string.a11y_unpacked)
    val quantityText = when {
        note.isNotBlank() -> "x${entry.quantity} • $note"
        entry.quantity > 1 -> "x${entry.quantity}"
        else -> ""
    }
    CatalogItemRow(
        name = displayName,
        note = quantityText,
        category = category,
        completedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.semantics {
            contentDescription = listOfNotNull(
                displayName,
                categoryLabel ?: unknownCategory,
                quantityDescription,
                note.takeIf { it.isNotBlank() },
                if (entry.isPacked) packed else unpacked,
            ).joinToString(", ")
            stateDescription = if (entry.isPacked) packed else unpacked
            role = Role.Checkbox
        },
        onClick = onToggle,
        role = Role.Checkbox,
        isCompleted = entry.isPacked,
        showCompletionIndicator = true,
        trailingContent = {
            PackingMetadataChip(note)
        },
    )
}

@Composable
private fun CatalogItemRow(
    name: String,
    note: String,
    category: PacklyCategory?,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    showCompletionIndicator: Boolean = false,
    completedIndicatorColor: androidx.compose.ui.graphics.Color? = null,
    role: Role? = null,
    minHeight: Dp = 52.dp,
    verticalPadding: Dp = PacklySpacing.base,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    val token = category?.let { CategoryTokens.byKey(it.key) } ?: CategoryTokens.byKey("misc")
    val indicatorColor = completedIndicatorColor ?: token.accent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = PacklySpacing.sm, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        if (showCompletionIndicator) {
            CompletionIndicator(isCompleted = isCompleted, accentColor = indicatorColor)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted) 0.60f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingContent()
        }
    }
}

@Composable
private fun CompletionIndicator(isCompleted: Boolean, accentColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (isCompleted) {
                    Modifier.background(accentColor, CircleShape)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun PackingMetadataChip(note: String) {
    // Short metadata such as "Digital" reads as a badge in the design references.
    if (note.isBlank() || note.length > 16 || note.contains(" ")) return
    Surface(
        shape = RoundedCornerShape(PacklyRadius.sm),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = note,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PacklySpacing.base, vertical = PacklySpacing.xs),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DuplicateWarningIcon() {
    val contentDescription = stringResource(R.string.a11y_duplicate_item_detected)
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun ItemRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    )
}
