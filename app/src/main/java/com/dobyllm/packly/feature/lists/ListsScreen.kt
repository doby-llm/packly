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
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.ui.component.EmptyState
import com.dobyllm.packly.ui.component.ListCard
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.SelectableItemCard
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val createListLabel = stringResource(R.string.action_create_list)
    val lists = doc.lists.filterNot { it.isArchived }

    DisposableEffect(onFabActionChange, createListLabel) {
        onFabActionChange?.invoke(PacklyFabAction(contentDescription = createListLabel, onClick = { showCreate = true }))
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
                        title = stringResource(R.string.lists_empty_title),
                        body = stringResource(R.string.lists_empty_body),
                        actionLabel = createListLabel,
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
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.list_duplicated_snackbar, list.name)) }
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
            title = { Text(stringResource(R.string.archive_list_title, list.name)) },
            text = { Text(stringResource(R.string.archive_list_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(list.id)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_archive)) }
            },
            dismissButton = { TextButton(onClick = { listToDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
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
            text = stringResource(R.string.lists_header_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.lists_header_body),
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
        title = { Text(stringResource(R.string.rename_list_title, list.name)) },
        text = {
            PacklyTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.field_list_name),
                supportingText = if (duplicateName) stringResource(R.string.error_duplicate_list_name) else null,
                isError = duplicateName,
            )
        },
        confirmButton = {
            TextButton(
                enabled = trimmedName.isNotEmpty() && !duplicateName,
                onClick = { onRename(trimmedName) },
            ) { Text(stringResource(R.string.action_rename)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun CreateListSheet(doc: PacklyAppDocument, onDismiss: () -> Unit, onCreate: (String, String, Set<ItemId>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var itemQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    val selected = remember { mutableStateListOf<ItemId>() }
    val activeItems = doc.items.filterNot { it.isArchived }
    val activeCategories = doc.categories
        .filterNot { it.isArchived }
        .filter { category -> activeItems.any { it.categoryId == category.id } }
        .sortedBy { it.sortOrder }
    val categoryLabelById = doc.categories.associate { it.id to it.label }
    val matchingItems = activeItems
        .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
        .filter { item ->
            val categoryLabel = categoryLabelById[item.categoryId].orEmpty()
            itemQuery.isBlank() ||
                item.name.contains(itemQuery, ignoreCase = true) ||
                item.notes.contains(itemQuery, ignoreCase = true) ||
                categoryLabel.contains(itemQuery, ignoreCase = true)
        }
    val itemSections = buildListBuilderItemSections(matchingItems, doc, stringResource(R.string.uncategorized))
    val duplicateName = doc.lists.any { !it.isArchived && it.name.equals(name.trim(), ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = PacklyRadius.xl, topEnd = PacklyRadius.xl),
    ) {
        Column(Modifier.fillMaxHeight(0.9f).navigationBarsPadding().imePadding()) {
            Text(
                stringResource(R.string.create_list_title),
                modifier = Modifier.padding(horizontal = PacklySpacing.md).padding(top = PacklySpacing.base, bottom = PacklySpacing.sm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PacklySpacing.md),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.base),
            ) {
                PacklyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.field_list_name),
                    supportingText = if (duplicateName) stringResource(R.string.error_duplicate_list_name) else null,
                    isError = duplicateName,
                )
                PacklyTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.field_description),
                )
                Text(
                    text = stringResource(R.string.checklist_items_selected, selected.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PacklyTextField(
                    value = itemQuery,
                    onValueChange = { itemQuery = it },
                    label = stringResource(R.string.search_items_count_label, activeItems.size),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PacklySpacing.base), verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                    PacklyCategoryFilterChip(
                        label = stringResource(R.string.filter_all_label),
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                    )
                    activeCategories.forEach { category ->
                        PacklyCategoryFilterChip(
                            label = category.label,
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == category.id) null else category.id },
                        )
                    }
                }
                if (itemSections.isEmpty()) {
                    Text(stringResource(R.string.no_items_match_filter), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    itemSections.forEach { section ->
                        Text(
                            text = stringResource(R.string.category_count_label, section.label, section.items.size),
                            modifier = Modifier.padding(top = PacklySpacing.base),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                            section.items.forEach { item ->
                                SelectableItemCard(
                                    title = item.name,
                                    subtitle = item.notes.takeIf { it.isNotBlank() } ?: section.label,
                                    selected = item.id in selected,
                                    onToggle = { if (item.id in selected) selected.remove(item.id) else selected.add(item.id) },
                                    compact = true,
                                )
                            }
                        }
                    }
                }
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
            ) { Text(stringResource(R.string.action_save_list)) }
        }
    }
}

@Composable
private fun PacklyCategoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(PacklyRadius.default),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
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

private fun buildListBuilderItemSections(
    items: List<PacklyItem>,
    doc: PacklyAppDocument,
    fallbackLabel: String,
): List<ListBuilderItemSection> {
    val categories = doc.categories.associateBy { it.id }
    return items
        .groupBy { it.categoryId }
        .map { (categoryId, categoryItems) ->
            val category = categories[categoryId]
            ListBuilderItemSection(
                categoryId = categoryId,
                label = category?.label ?: fallbackLabel,
                sortOrder = category?.sortOrder ?: Int.MAX_VALUE,
                items = categoryItems.sortedBy { it.name.lowercase() },
            )
        }
        .sortedWith(compareBy<ListBuilderItemSection> { it.sortOrder }.thenBy { it.label })
}

private data class ListBuilderItemSection(
    val categoryId: CategoryId,
    val label: String,
    val sortOrder: Int,
    val items: List<PacklyItem>,
)

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
