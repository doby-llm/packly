@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dobyllm.packly.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.ThemeMode

@Composable
fun HomeScreen(
    doc: PacklyAppDocument,
    onItems: () -> Unit,
    onLists: () -> Unit,
    onTrips: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    var showAppearance by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Packly") },
                actions = {
                    IconButton(onClick = { showAppearance = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Appearance")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ready to pack?", style = MaterialTheme.typography.headlineMedium)
                Text("Choose what you want to prepare.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DestinationCard(Icons.Rounded.Backpack, "Trips", "${doc.trips.count { it.status.name != "Archived" }} packing sessions", onTrips)
                DestinationCard(Icons.Rounded.Checklist, "Lists", "${doc.lists.count { !it.isArchived }} reusable templates", onLists)
                DestinationCard(Icons.Rounded.EditNote, "Items", "${doc.items.count { !it.isArchived }} reusable packing items", onItems)
            }
        }
    }

    if (showAppearance) {
        AppearanceDialog(
            selectedMode = doc.settings.themeMode,
            onSelect = { mode ->
                onThemeModeChange(mode)
                showAppearance = false
            },
            onDismiss = { showAppearance = false },
        )
    }
}

@Composable
private fun AppearanceDialog(selectedMode: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Appearance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = selectedMode == mode, onClick = { onSelect(mode) })
                        Column {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(mode.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.System -> "Follow system"
        ThemeMode.Light -> "Light"
        ThemeMode.Dark -> "Dark"
    }

private val ThemeMode.description: String
    get() = when (this) {
        ThemeMode.System -> "Use your device theme."
        ThemeMode.Light -> "Always use light surfaces and dark system icons."
        ThemeMode.Dark -> "Always use dark surfaces and light system icons."
    }

@Composable
private fun DestinationCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
