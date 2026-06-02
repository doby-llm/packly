@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.items

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.PacklyCategory

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EditItemSheet(categories: List<PacklyCategory>, onDismiss: () -> Unit, onSave: (String, CategoryId, Int, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var categoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add item", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Name (required)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).ifBlank { "1" } }, label = { Text("Default quantity") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.filterNot { it.isArchived }.forEach { category ->
                    FilterChip(selected = category.id == categoryId, onClick = { categoryId = category.id }, label = { Text(category.label) })
                }
            }
            Button(enabled = name.trim().isNotEmpty() && categoryId.isNotBlank(), onClick = { onSave(name, categoryId, quantity.toIntOrNull() ?: 1, notes); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Save item") }
        }
    }
}
