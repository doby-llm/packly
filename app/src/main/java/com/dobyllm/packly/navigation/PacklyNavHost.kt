package com.dobyllm.packly.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dobyllm.packly.PacklyAppViewModel
import com.dobyllm.packly.core.model.ThemeMode
import com.dobyllm.packly.feature.home.HomeScreen
import com.dobyllm.packly.feature.items.ItemsScreen
import com.dobyllm.packly.feature.lists.ListDetailScreen
import com.dobyllm.packly.feature.lists.ListsScreen
import com.dobyllm.packly.feature.packing.PackingModeScreen
import com.dobyllm.packly.feature.trips.TripDetailScreen
import com.dobyllm.packly.feature.trips.TripsScreen
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.PacklyScaffold
import com.dobyllm.packly.ui.component.PacklyTopLevelDestinations

@Composable
fun PacklyNavHost(
    navController: NavHostController = rememberNavController(),
    vm: PacklyAppViewModel = viewModel(),
    initialTripId: String? = null,
) {
    val doc = vm.document.collectAsStateWithLifecycle().value
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = PacklyTopLevelDestinations.any { it.route == currentRoute }
    var showAppearance by remember { mutableStateOf(false) }
    var fabAction by remember { mutableStateOf<PacklyFabAction?>(null) }

    LaunchedEffect(initialTripId) {
        initialTripId?.let { navController.navigate(PacklyRoute.tripDetail(it)) }
    }

    // Route changes must clear screen-scoped actions so nested destinations never inherit a FAB.
    LaunchedEffect(currentRoute) {
        fabAction = null
    }

    PacklyScaffold(
        currentRoute = currentRoute,
        canNavigateBack = !isTopLevelRoute,
        nestedTitle = currentRoute.nestedTitle(),
        fabAction = fabAction,
        onBack = { navController.popBackStack() },
        onSettings = { showAppearance = true },
        onDestinationClick = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { shellPadding ->
        NavHost(
            navController = navController,
            startDestination = PacklyRoute.Home,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(PacklyRoute.Home) {
                HomeScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onItems = { navController.navigate(PacklyRoute.Items) },
                    onLists = { navController.navigate(PacklyRoute.Lists) },
                    onTrips = { navController.navigate(PacklyRoute.Trips) },
                )
            }
            composable(PacklyRoute.Items) {
                ItemsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { fabAction = it },
                    onAdd = vm::addItem,
                    onUpdate = vm::updateItem,
                    onDelete = vm::archiveItem,
                )
            }
            composable(PacklyRoute.Lists) {
                ListsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { fabAction = it },
                    onCreate = vm::createList,
                    onOpen = { navController.navigate(PacklyRoute.listDetail(it)) },
                    onUseForTrip = { listId ->
                        vm.createTrip("Trip from list", "", listId, emptySet())
                        navController.navigate(PacklyRoute.Trips)
                    },
                    onDelete = vm::deleteList,
                )
            }
            composable(PacklyRoute.ListDetail) { backStack ->
                val id = backStack.arguments?.getString("listId") ?: ""
                ListDetailScreen(
                    doc = doc,
                    listId = id,
                    contentPadding = shellPadding,
                    onToggle = { itemId -> vm.toggleListItem(id, itemId) },
                )
            }
            composable(PacklyRoute.Trips) {
                TripsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { fabAction = it },
                    onCreate = vm::createTrip,
                    onOpen = { navController.navigate(PacklyRoute.tripDetail(it)) },
                    onPack = { navController.navigate(PacklyRoute.packingMode(it)) },
                    onDelete = vm::deleteTrip,
                )
            }
            composable(PacklyRoute.TripDetail) { backStack ->
                val id = backStack.arguments?.getString("tripId") ?: ""
                TripDetailScreen(
                    doc = doc,
                    tripId = id,
                    contentPadding = shellPadding,
                    onPack = { navController.navigate(PacklyRoute.packingMode(id)) },
                    onReset = { vm.resetPacking(id) },
                    onQuantityChange = { entryId, quantity -> vm.updateTripEntryQuantity(id, entryId, quantity) },
                    onDeadlineChange = { deadline -> vm.updateTripDeadline(id, deadline) },
                )
            }
            composable(PacklyRoute.PackingMode) { backStack ->
                val id = backStack.arguments?.getString("tripId") ?: ""
                PackingModeScreen(
                    doc = doc,
                    tripId = id,
                    contentPadding = shellPadding,
                    onToggle = { vm.togglePacked(id, it) },
                )
            }
        }
    }

    if (showAppearance) {
        AppearanceDialog(
            selectedMode = doc.settings.themeMode,
            onSelect = { mode ->
                vm.updateThemeMode(mode)
                showAppearance = false
            },
            onDismiss = { showAppearance = false },
        )
    }
}

private fun String?.nestedTitle(): String = when (this) {
    PacklyRoute.ListDetail -> "List"
    PacklyRoute.TripDetail -> "Trip"
    PacklyRoute.PackingMode -> "Packing"
    else -> "Packly"
}

@Composable
private fun AppearanceDialog(selectedMode: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Settings") },
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
                            Text(
                                mode.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
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
