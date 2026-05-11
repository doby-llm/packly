package com.packly.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.packly.app.ui.components.categoryColor
import com.packly.app.ui.components.categoryIcon
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Tinder creation mode — swipeable cards showing one backlog item at a time.
 * Swipe RIGHT to add to list, LEFT to skip.
 * After the last card, all remaining unswiped items are auto-added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinderCreateScreen(
    listName: String,
    repository: PacklyRepository,
    onBack: () -> Unit,
    onListCreated: (ItemList) -> Unit
) {
    val allItems by repository.getItems().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val scope = rememberCoroutineScope()

    // Shuffle items for a fun experience; stable across recompositions
    val shuffledItems = remember(allItems) {
        if (allItems.isEmpty()) emptyList()
        else allItems.shuffled()
    }

    var currentIndex by remember { mutableStateOf(0) }
    var addedItemIds by remember { mutableStateOf(setOf&lt;String&gt;()) }
    var swipeState by remember { mutableStateOf&lt;TinderAction&gt;(TinderAction.NONE) }

    val remaining = shuffledItems.size - currentIndex
    val currentItem = shuffledItems.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tinder: $listName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Remaining count badge
            Text(
                text = "$remaining item${
                    if (remaining != 1) "s" else ""
                } remaining",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (currentItem == null || shuffledItems.isEmpty()) {
                // All done or empty backlog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (allItems.isEmpty()) "No items in backlog yet.\nAdd some items first!"
                                   else "All done!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (addedItemIds.isNotEmpty())
                                   "${addedItemIds.size} item${if (addedItemIds.size != 1) "s" else ""} added to your list."
                                   else "No items were added.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = {
                            scope.launch {
                                val entries = addedItemIds.map { itemId ->
                                    ListEntry(itemId = itemId, quantity = 1)
                                }
                                val list = ItemList(
                                    id = UUID.randomUUID().toString(),
                                    name = listName,
                                    mode = "tinder",
                                    items = entries,
                                    createdAt = System.currentTimeMillis()
                                )
                                repository.createList(list)
                                onListCreated(list)
                            }
                        }) {
                            Text("Create List")
                        }
                    }
                }
            } else {
                // Swipeable card area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SwipeableCard(
                        item = currentItem,
                        category = categoryMap[currentItem.categoryId],
                        onSwipedRight = {
                            addedItemIds = addedItemIds + currentItem.id
                            swipeState = TinderAction.ADDED
                            currentIndex++
                        },
                        onSwipedLeft = {
                            swipeState = TinderAction.SKIPPED
                            currentIndex++
                        }
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = {
                            swipeState = TinderAction.SKIPPED
                            currentIndex++
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Close, "Skip", modifier = Modifier.size(28.dp))
                    }

                    FilledTonalButton(
                        onClick = {
                            if (currentItem != null) {
                                addedItemIds = addedItemIds + currentItem.id
                            }
                            swipeState = TinderAction.ADDED
                            currentIndex++
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Favorite, "Add", modifier = Modifier.size(28.dp))
                    }
                }

                // Swipe feedback indicator
                if (swipeState != TinderAction.NONE) {
                    val feedbackColor = when (swipeState) {
                        TinderAction.ADDED -> MaterialTheme.colorScheme.primary
                        TinderAction.SKIPPED -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }
                    val feedbackText = when (swipeState) {
                        TinderAction.ADDED -> "Added!"
                        TinderAction.SKIPPED -> "Skipped"
                        else -> ""
                    }
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.labelLarge,
                        color = feedbackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LaunchedEffect(swipeState) {
                        kotlinx.coroutines.delay(400)
                        swipeState = TinderAction.NONE
                    }
                }
            }
        }
    }
}

private enum class TinderAction { NONE, ADDED, SKIPPED }

/**
 * A single swipeable card that shows an item's category, icon, and name.
 * Drag right to add, left to skip.
 */
@Composable
private fun SwipeableCard(
    item: Item,
    category: com.packly.app.data.model.Category?,
    onSwipedRight: () -> Unit,
    onSwipedLeft: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val swipeThreshold = 300f // px threshold to trigger a swipe

    val bgColor = category?.let { categoryColor(it) } ?: MaterialTheme.colorScheme.primary
    val icon = category?.let { categoryIcon(it) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = rotation.value
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > swipeThreshold) {
                                offsetX.animateTo(
                                    targetValue = 1200f,
                                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                                )
                                onSwipedRight()
                            } else if (offsetX.value 