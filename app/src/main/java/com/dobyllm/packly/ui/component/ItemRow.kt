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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.TripEntry
import com.dobyllm.packly.ui.token.CategoryTokens
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
fun PackingItemRow(entry: TripEntry, category: PacklyCategory?, onToggle: () -> Unit) {
    CatalogItemRow(
        name = entry.nameSnapshot,
        note = listOfNotNull(
            category?.label,
            entry.notes.takeIf { it.isNotBlank() },
        ).joinToString(" • "),
        category = category,
        isCompleted = entry.isPacked,
        modifier = Modifier.semantics {
            contentDescription = "${entry.nameSnapshot}, ${category?.label ?: "Unknown category"}, quantity ${entry.quantity}, ${if (entry.isPacked) "packed" else "not packed"}"
            stateDescription = if (entry.isPacked) "packed" else "not packed"
        },
        onClick = onToggle,
        role = Role.Checkbox,
        trailingContent = { if (entry.quantity > 1) QuantityBadge(entry.quantity) },
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
            .heightIn(min = 48.dp)
            .clickable(role = role, onClick = onClick)
            .padding(vertical = PacklySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.marginMobile),
    ) {
        CompletionIndicator(isCompleted = isCompleted, accentColor = token.accent)
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (isCompleted) 0.60f else 1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .size(24.dp)
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
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
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
