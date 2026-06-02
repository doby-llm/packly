@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.component.SelectableItemCard
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun AddItemsToListSheet(doc: PacklyAppDocument, selectedIds: Set<String>, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(PacklySpacing.md),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            item {
                Text(
                    "Select items",
                    modifier = Modifier.padding(bottom = PacklySpacing.xs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(doc.items.filterNot { it.isArchived }, key = { it.id }) { item ->
                val category = doc.categories.firstOrNull { it.id == item.categoryId }
                SelectableItemCard(
                    title = item.name,
                    subtitle = listOfNotNull(category?.label, item.notes.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Uncategorized" },
                    selected = item.id in selectedIds,
                    onToggle = { onToggle(item.id) },
                )
            }
        }
    }
}
