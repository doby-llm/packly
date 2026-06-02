@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.ui.component.CategoryChip

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EditItemSheet(
    categories: List<PacklyCategory>,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, CategoryId, String) -> Unit,
    item: PacklyItem? = null,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var notes by remember(item?.id) { mutableStateOf(item?.notes.orEmpty()) }
    var categoryId by remember(categories, item?.id) { mutableStateOf(item?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    val trimmedName = name.trim()
    val duplicateName = existingNames.any { it.equals(trimmedName, ignoreCase = true) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (item == null) "Add item" else "Edit item", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Name (required)") },
                supportingText = { if (duplicateName) Text("An active item with this name already exists.") },
                isError = duplicateName,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.filterNot { it.isArchived }.forEach { category ->
                    CategoryChip(
                        label = category.label,
                        selected = category.id == categoryId,
                        onClick = { categoryId = category.id },
                    )
                }
            }
            Button(
                enabled = trimmedName.isNotEmpty() && categoryId.isNotBlank() && !duplicateName,
                onClick = {
                    onSave(trimmedName, categoryId, notes)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (item == null) "Save item" else "Update item") }
        }
    }
}
