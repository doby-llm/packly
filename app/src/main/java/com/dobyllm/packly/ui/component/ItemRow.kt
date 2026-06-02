package com.dobyllm.packly.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.TripEntry

@Composable
fun ItemRow(item: PacklyItem, category: PacklyCategory?, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(listOfNotNull(category?.label, item.notes.takeIf { it.isNotBlank() }).joinToString(" • ")) },
        trailingContent = {
            Row { IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit ${item.name}") }; IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete ${item.name}") } }
        },
    )
}

@Composable
fun PackingItemRow(entry: TripEntry, category: PacklyCategory?, onToggle: () -> Unit) {
    val description = "${entry.nameSnapshot}, ${category?.label ?: "Unknown category"}, quantity ${entry.quantity}, ${if (entry.isPacked) "packed" else "not packed"}"
    ListItem(
        modifier = Modifier.clickable(role = Role.Checkbox) { onToggle() }.semantics { contentDescription = description },
        leadingContent = { Checkbox(checked = entry.isPacked, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(entry.nameSnapshot) },
        supportingContent = { Text(category?.label ?: "Unknown") },
        trailingContent = { if (entry.quantity > 1) QuantityBadge(entry.quantity) },
    )
}
