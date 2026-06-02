package com.dobyllm.packly.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dobyllm.packly.PacklyAppViewModel
import com.dobyllm.packly.feature.home.HomeScreen
import com.dobyllm.packly.feature.items.ItemsScreen
import com.dobyllm.packly.feature.lists.ListDetailScreen
import com.dobyllm.packly.feature.lists.ListsScreen
import com.dobyllm.packly.feature.packing.PackingModeScreen
import com.dobyllm.packly.feature.trips.TripDetailScreen
import com.dobyllm.packly.feature.trips.TripsScreen

@Composable
fun PacklyNavHost(navController: NavHostController = rememberNavController(), vm: PacklyAppViewModel = viewModel()) {
    val doc = vm.document.collectAsStateWithLifecycle().value
    NavHost(
        navController = navController,
        startDestination = PacklyRoute.Home,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        composable(PacklyRoute.Home) {
            HomeScreen(
                doc,
                onItems = { navController.navigate(PacklyRoute.Items) },
                onLists = { navController.navigate(PacklyRoute.Lists) },
                onTrips = { navController.navigate(PacklyRoute.Trips) },
                onThemeModeChange = vm::updateThemeMode,
            )
        }
        composable(PacklyRoute.Items) { ItemsScreen(doc, onBack = { navController.popBackStack() }, onAdd = vm::addItem, onUpdate = vm::updateItem, onDelete = vm::archiveItem) }
        composable(PacklyRoute.Lists) { ListsScreen(doc, onBack = { navController.popBackStack() }, onCreate = vm::createList, onOpen = { navController.navigate(PacklyRoute.listDetail(it)) }, onUseForTrip = { listId -> vm.createTrip("Trip from list", "", listId, emptySet()); navController.navigate(PacklyRoute.Trips) }, onDelete = vm::deleteList) }
        composable(PacklyRoute.ListDetail) { backStack -> val id = backStack.arguments?.getString("listId") ?: ""; ListDetailScreen(doc, id, onBack = { navController.popBackStack() }, onToggle = { itemId -> vm.toggleListItem(id, itemId) }) }
        composable(PacklyRoute.Trips) { TripsScreen(doc, onBack = { navController.popBackStack() }, onCreate = vm::createTrip, onOpen = { navController.navigate(PacklyRoute.tripDetail(it)) }, onPack = { navController.navigate(PacklyRoute.packingMode(it)) }, onDelete = vm::deleteTrip) }
        composable(PacklyRoute.TripDetail) { backStack -> val id = backStack.arguments?.getString("tripId") ?: ""; TripDetailScreen(doc, id, onBack = { navController.popBackStack() }, onPack = { navController.navigate(PacklyRoute.packingMode(id)) }, onReset = { vm.resetPacking(id) }) }
        composable(PacklyRoute.PackingMode) { backStack -> val id = backStack.arguments?.getString("tripId") ?: ""; PackingModeScreen(doc, id, onBack = { navController.popBackStack() }, onToggle = { vm.togglePacked(id, it) }) }
    }
}
