package com.packly.app.ui.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.ListEntry
import com.packly.app.data.repository.PacklyRepository
import com.packly.app.ui.components.CategoryBadge
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinderCreateScreen(
    listName: String,
    repository: PacklyRepository,
    onBack: () -> Unit,
    onListCreated: (ItemList) -> Unit
) {
    val items by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var currentIndex by remember { mutableStateOf(0) }
    var includedIds by remember { mutableStateOf<Set<String>>(setOf()) }
    var excludedIds by remember { mutableStateOf<Set<String>>(setOf()) }
    var finished by remember { mutableStateOf(false) }

    val totalItems = filteredItems.size
    val currentItem = filteredItems.getOrNull(currentIndex)

    var dragOffset by remember { mutableStateOf(0f) }

    val rotation = dragOffset / 20f
    val includeAlpha = (dragOffset / 300f).coerceIn(0f, 0.9f)
    val excludeAlpha = (-dragOffset / 300f).coerceIn(0f, 0.9f)

    fun swipeRight(item: Item) {
        includedIds = includedIds + item.id
        if (currentIndex + 1 >= totalItems) finished = true
        else currentIndex++
        dragOffset = 0f
    }

    fun swipeLeft(item: Item) {
        excludedIds = excludedIds + item.id
        if (currentIndex + 1 >= totalItems) finished = true
        else currentIndex++
        dragOffset = 0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Tinder: $listName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (currentIndex >= totalItems || finished) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${includedIds.size} item${if (includedIds.size != 1) "s" else ""} selected",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (excludedIds.isNotEmpty()) {
                                Text(
                                    text = "${excludedIds.size} skipped",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val entries = includedIds.map { itemId ->
                                        ListEntry(itemId = itemId, quantity = 1)
                                    }
                                    val list = ItemList(
                                        id = UUID.randomUUID().toString(),
                                        name = listName,
                                        items = entries,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.createList(list)
                                    onListCreated(list)
                                }
                            },
                            enabled = includedIds.isNotEmpty()
                        ) {
                            Text(text = "Create List (${includedIds.size})")
                        }
                    }
                }
            } else {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentIndex + 1} / $totalItems",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${includedIds.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    currentIndex = 0
                    dragOffset = 0f
                    finished = false
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = "Search items...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            when {
                filteredItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No items match your search"
                                   else "Backlog is empty. Add items first!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                currentIndex >= totalItems -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "All done!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You selected ${includedIds.size} of $totalItems items for \"$listName\".",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        if (excludedIds.isNotEmpty()) {
                            Text(
                                text = "${excludedIds.size} items were skipped.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val item = currentItem ?: return@Scaffold
                        val cat = categoryMap[item.categoryId]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp)
                                .rotate(rotation)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (abs(dragOffset) > 200f) {
                                                if (dragOffset > 0) swipeRight(item)
                                                else swipeLeft(item)
                                            } else {
                                                dragOffset = 0f
                                            }
                                        },
                                        onDragCancel = { dragOffset = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            dragOffset = (dragOffset + dragAmount)
                                                .coerceIn(-400f, 400f)
                                        }
                                    )
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(Modifier.height(60.dp))

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    if (cat != null) {
                                        CategoryBadge(category = cat)
                                    }

                                    Spacer(Modifier.height(40.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${currentIndex + 1} / $totalItems",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (dragOffset > 50f) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(16.dp),
                                        color = Color(0xFF4CAF50).copy(alpha = includeAlpha),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text(
                                            text = "INCLUDE",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                if (dragOffset < -50f) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(16.dp),
                                        color = Color(0xFFF44336).copy(alpha = excludeAlpha),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text(
                                            text = "EXCLUDE",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
