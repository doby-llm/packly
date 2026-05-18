package com.packly.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.packly.app.data.repository.PacklyRepository
import com.packly.app.ui.screens.ChecklistCreateScreen
import com.packly.app.ui.screens.HomeScreen
import com.packly.app.ui.screens.ItemsScreen
import com.packly.app.ui.screens.ListsScreen
import com.packly.app.ui.screens.TripsListScreen
import com.packly.app.ui.screens.TripDetailScreen
import kotlinx.coroutines.launch

@Composable
fun PacklyNavGraph(
    navController: NavHostController,
    repository: PacklyRepository
) {
    NavHost(navController = navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onEditItems = { navController.navigate(NavRoutes.EDIT_ITEMS) },
                onLists = { navController.navigate(NavRoutes.LISTS) },
                onTrips = { navController.navigate(NavRoutes.TRIPS) }
            )
        }
        composable(NavRoutes.EDIT_ITEMS) {
            ItemsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.LISTS) {
            ListsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.TRIPS) {
            TripsListScreen(
                repository = repository,
                onTripClick = { tripId ->
                    navController.navigate(NavRoutes.tripDetail(tripId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripDetailScreen(
                tripId = tripId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.CREATE_LIST_CHECKLIST,
            arguments = listOf(navArgument("listName") { type = NavType.StringType })
        ) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString("listName") ?: return@composable
            val scope = rememberCoroutineScope()
            ChecklistCreateScreen(
                listName = listName,
                repository = repository,
                onBack = { navController.popBackStack() },
                onListCreated = { createdList ->
                    scope.launch {
                        repository.createList(createdList)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}