@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.ui.component.CategoryChip
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PacklySpacing.md, vertical = PacklySpacing.sm)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            Text(
                if (item == null) stringResource(R.string.item_sheet_add_title) else stringResource(R.string.item_sheet_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name_required)) },
                supportingText = { if (duplicateName) Text(stringResource(R.string.error_duplicate_item_name)) },
                isError = duplicateName,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = packlyTextFieldColors(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = packlyTextFieldColors(),
            )
            Text(stringResource(R.string.field_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
            ) {
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
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(if (item == null) stringResource(R.string.action_save_item) else stringResource(R.string.action_update_item))
            }
        }
    }
}

@Composable
private fun packlyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
)
