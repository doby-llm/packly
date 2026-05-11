package com.packly.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.packly.app.model.Category
import com.packly.app.model.PackingItem
import com.packly.app.ui.components.categoryIcon

// ──────────────────────────────────────────────────────────────────────────
// Screen 3: Add / Edit Item — modal bottom sheet.
// User enters name, picks category, adjusts quantity, optional notes.
// ──────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditItemSheet(
    existingItem: PackingItem? = null, // null = add mode, non-null = edit mode
    onDismiss: () -> Unit,
    onSave: (PackingItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = existingItem != null

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(existingItem?.category ?: Category.CLOTHING_TOP) }
    var quantity by remember { mutableIntStateOf(existingItem?.quantity ?: 1) }
    var notes by remember { mutableStateOf(existingItem?.notes ?: "") }

    val isValid = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isEditing) "Edit Item" else "Add Item",
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Item name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item name") },
                placeholder = { Text("e.g. Blue t-shirt") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Category picker
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Quantity stepper
            Text(
                text = "Quantity",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (quantity > 1) quantity-- },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    onClick = { if (quantity &lt; 99) quantity++ },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Notes (optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Don't forget the USB-C cable") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(28.dp))

            // Save button
            Button(
                onClick = {
                    val item = PackingItem(
                        id = existingItem?.id, // preserve id on edit
                        name = name.trim(),
                        category = selectedCategory,
                        quantity = quantity,
                        isPacked = existingItem?.isPacked ?: false,
                        notes = notes.trim(),
                    )
                    onSave(item)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add to List",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
