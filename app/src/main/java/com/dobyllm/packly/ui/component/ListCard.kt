package com.dobyllm.packly.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyList

@Composable
fun ListCard(list: PacklyList, onOpen: () -> Unit, onUseForTrip: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(list.name, style = MaterialTheme.typography.titleMedium)
            Text("${list.entries.size} items${if (list.description.isNotBlank()) " • ${list.description}" else ""}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUseForTrip) { Text("Use for trip") }
                OutlinedButton(onClick = onOpen) { Text("Edit") }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    }
}
