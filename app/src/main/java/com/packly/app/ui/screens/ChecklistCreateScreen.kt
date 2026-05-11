package com.packly.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.ListEntry
import com.packly.app.data.repository.PacklyRepository
import com.packly.app.ui.components.CategoryBadge
import com.packly.app.ui.components.categoryColor
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Checklist creation mode — all backlog items grouped by category with checkboxes.
 * User checks items they want, then taps "Create List" to save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistCreateScreen(
    listName: String,
    repository: PacklyRepository,
    onBack: () -> Unit,
    onListCreated: (ItemList) -> Unit
) {
    val items by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var checkedIds by remember { mutableStateOf(setOf