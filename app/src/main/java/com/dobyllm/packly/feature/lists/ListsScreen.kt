@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.dobyllm.packly.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ListCard
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(
    doc: PacklyAppDocument,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onFabActionChange: ((PacklyFabAction?) -> Unit)? = null,
    onCreate: (String, String, Set<ItemId>) -> Unit,
    onOpen: (ListId) -> Unit,
    onRename: (ListId, String) -> Unit,
    onDuplicate: (ListId) -> Unit,
    onDelete: (ListId) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<PacklyList?>(null) }
    var listToDelete by remember { mutableStateOf<PacklyList?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lists = doc.lists.filterNot { it.isArchived }

    DisposableEffect(onFabActionChange) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = "Create list", onClick = { showCreate = true }))
        onDispose { onFabActionChange?.invoke(null) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(PacklySpacing.marginMobile),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                ListsHeader()
            }
            if (lists.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "No packing lists yet",
                        body = "Start with a reusable template for trips you take often.",
                        actionLabel = "Create list",
                        onAction = { showCreate = true },
                    )
                }
            }
            items(lists, key = { it.id }) { list ->
                ListCard(
                    list = list,
                    categories = doc.categories,
                    onOpen = { onOpen(list.id) },
                    onRename = { listToRename = list },
                    onDuplicate = {
                        onDuplicate(list.id)
                        scope.launch { snackbarHostState.showSnackbar("${list.name} duplicated") }
                    },
                    onDelete = { listToDelete = list },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .padding(PacklySpacing.md),
        )
    }

    if (showCreate) CreateListSheet(doc, onDismiss = { showCreate = false }, onCreate = onCreate)
    listToRename?.let { list ->
        RenameListDialog(
            list = list,
            existingNames = lists.filter { it.id != list.id }.map { it.name },
            onDismiss = { listToRename = null },
            onRename = { name ->
                onRename(list.id, name)
                listToRename = null
            },
        )
    }
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Archive ${list.name}?") },
            text = { Text("The list will be hidden, but trips already created from it keep their snapshots.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(list.id)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { listToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ListsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PacklySpacing.base),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
    ) {
        Text(
            text = "My Lists",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Manage your packing templates and upcoming trips.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameListDialog(
    list: PacklyList,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(list.id) { mutableStateOf(list.name) }
    val trimmedName = name.trim()
    val duplicateName = existingNames.any { it.equals(trimmedName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${list.name}") },
        text = {
            PacklyTextField(
                value = name,
                onValueChange = { name = it },
                label = "List name",
                supportingText = if (duplicateName) "An active list with this name already exists." else null,
                isError = duplicateName,
            )
        },
        confirmButton = {
            TextButton(
                enabled = trimmedName.isNotEmpty() && !duplicateName,
                onClick = { onRename(trimmedName) },
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CreateListSheet(doc: PacklyAppDocument, onDismiss: () -> Unit, onCreate: (String, String, Set<ItemId>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var itemQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ItemId>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val matchingItems = activeItems.filter { it.name.contains(itemQuery, ignoreCase = true) }
    val duplicateName = doc.lists.any { !it.isArchived && it.name.equals(name.trim(), ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding().imePadding()) {
            Text(
                "Create list",
                modifier = Modifier.padding(horizontal = PacklySpacing.md).padding(top = PacklySpacing.base, bottom = PacklySpacing.sm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PacklySpacing.md),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            ) {
                PacklyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "List name",
                    supportingText = if (duplicateName) "An active list with this name already exists." else null,
                    isError = duplicateName,
                )
                PacklyTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                )
                Text("Checklist items", style = MaterialTheme.typography.labelLarge)
                PacklyTextField(
                    value = itemQuery,
                    onValueChange = { itemQuery = it },
                    label = "Search all ${activeItems.size} items",
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                    matchingItems.forEach { item ->
                        FilterChip(
                            selected = item.id in selected,
                            onClick = { if (item.id in selected) selected.remove(item.id) else selected.add(item.id) },
                            label = { Text(item.name) },
                            shape = RoundedCornerShape(PacklyRadius.default),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = item.id in selected,
                                borderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryFixed,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                            ),
                        )
                    }
                }
                if (matchingItems.isEmpty()) Text("No items match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                enabled = name.trim().isNotEmpty() && !duplicateName,
                onClick = {
                    onCreate(name, description, selected.toSet())
                    onDismiss()
                },
                modifier = Modifier.padding(PacklySpacing.md).fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(PacklyRadius.default),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Save list") }
        }
    }
}

@Composable
private fun PacklyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.default),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    )
}
