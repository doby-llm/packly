package com.dobyllm.packly.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyList

@Composable
fun ListCard(list: PacklyList, onOpen: () -> Unit, onUseForTrip: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(list.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More actions for ${list.name}")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit details") },
                            onClick = {
                                showMenu = false
                                onOpen()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        )
                    }
                }
            }
            Text("${list.entries.size} items${if (list.description.isNotBlank()) " • ${list.description}" else ""}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onUseForTrip, modifier = Modifier.align(Alignment.End)) { Text("Use for trip") }
        }
    }
}
