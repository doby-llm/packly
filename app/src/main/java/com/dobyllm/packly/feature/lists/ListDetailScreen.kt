@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun ListDetailScreen(
    doc: PacklyAppDocument,
    listId: ListId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onToggle: (ItemId) -> Unit,
) {
    val list = doc.lists.firstOrNull { it.id == listId }
    val selected = list?.entries?.mapNotNull { it.itemId }?.toSet() ?: emptySet()

    if (list == null) {
        Text("List not found", Modifier.padding(contentPadding).padding(20.dp))
    } else {
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            contentPadding = PaddingValues(PacklySpacing.marginMobile),
        ) {
            item {
                Text(
                    "Select items from your library. Changes snapshot names and quantities into this template.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(doc.categories.firstOrNull { it.id == item.categoryId }?.label ?: "Unknown") },
                    leadingContent = { Checkbox(item.id in selected, onCheckedChange = { onToggle(item.id) }) },
                )
            }
        }
    }
}
