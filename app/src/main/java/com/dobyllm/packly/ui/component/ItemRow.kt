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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.TripEntry
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
    CatalogItemRow(
        name = item.name,
        note = item.notes,
        category = category,
        isCompleted = false,
        modifier = modifier,
        onClick = onEdit,
        trailingContent = {
            if (hasDuplicate) DuplicateWarningIcon()
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Archive ${item.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val quantityText = when {
        note.isNotBlank() -> "x${entry.quantity} • $note"
        entry.quantity > 1 -> "x${entry.quantity}"
        else -> ""
    }
    CatalogItemRow(
        name = entry.nameSnapshot,
        note = quantityText,
        category = category,
        isCompleted = entry.isPacked,
        modifier = modifier.semantics {
            contentDescription = listOfNotNull(
                entry.nameSnapshot,
                category?.label ?: "Unknown category",
                "quantity ${entry.quantity}",
                note.takeIf { it.isNotBlank() },
                if (entry.isPacked) "packed" else "unpacked",
            ).joinToString(", ")
            stateDescription = if (entry.isPacked) "packed" else "unpacked"
            role = Role.Checkbox
        },
        onClick = onToggle,
        role = Role.Checkbox,
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
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    role: Role? = null,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    val token = category?.let { CategoryTokens.byKey(it.key) } ?: CategoryTokens.byKey("misc")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        CompletionIndicator(isCompleted = isCompleted, accentColor = token.accent)
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
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Duplicate item detected" },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
