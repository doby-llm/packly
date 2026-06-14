package com.dobyllm.packly.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.dobyllm.packly.ListCopyNameTemplates
import com.dobyllm.packly.PacklyAppViewModel
import com.dobyllm.packly.R
import com.dobyllm.packly.feature.home.HomeScreen
import com.dobyllm.packly.feature.items.ItemsScreen
import com.dobyllm.packly.feature.lists.ListDetailScreen
import com.dobyllm.packly.feature.lists.ListsScreen
import com.dobyllm.packly.feature.options.OptionsScreen
import com.dobyllm.packly.feature.packing.PackingModeScreen
import com.dobyllm.packly.feature.trips.TripDetailScreen
import com.dobyllm.packly.feature.trips.TripsScreen
import com.dobyllm.packly.feature.trips.create.CreateTripDeadlineScreen
import com.dobyllm.packly.feature.trips.create.CreateTripDetailsScreen
import com.dobyllm.packly.feature.trips.create.CreateTripItemsScreen
import com.dobyllm.packly.feature.trips.create.CreateTripListsScreen
import com.dobyllm.packly.feature.trips.create.rememberCreateTripDraftState
import com.dobyllm.packly.ui.component.PacklyFabAction
import com.dobyllm.packly.ui.component.PacklyScaffold
import com.dobyllm.packly.ui.component.PacklyTopBarAction
import com.dobyllm.packly.ui.component.packlyTopLevelDestinations
import com.dobyllm.packly.ui.token.PacklyMotion

@Composable
fun PacklyNavHost(
    navController: NavHostController = rememberNavController(),
    vm: PacklyAppViewModel = viewModel(),
    initialTripId: String? = null,
) {
    val doc = vm.document.collectAsStateWithLifecycle().value
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = packlyTopLevelDestinations().any { it.route == currentRoute }
    var routeFabAction by remember { mutableStateOf<RouteFabAction?>(null) }
    val fabAction = routeFabAction?.takeIf { it.route == currentRoute }?.action
    val isTripDetailRoute = currentRoute == PacklyRoute.TripDetail
    val createTripDraftState = rememberCreateTripDraftState()
    val tripFromListDefaultName = stringResource(R.string.trip_from_list_default_name)
    val listCopyNameTemplates = ListCopyNameTemplates(
        unnumberedTemplate = stringResource(R.string.list_duplicate_copy_name),
        numberedTemplate = stringResource(R.string.list_duplicate_copy_name_numbered),
    )

    fun exitCreateTrip() {
        createTripDraftState.discard()
        navController.popBackStack()
    }

    fun startCreateTrip() {
        createTripDraftState.discard()
        navController.navigate(PacklyRoute.CreateTripGraph)
    }

    fun navigateSafely(route: String?) {
        route?.let { navController.navigate(it) }
    }

    fun navigateToTripDetail(tripId: String) {
        if (doc.trips.any { it.id == tripId }) navigateSafely(PacklyRoute.tripDetail(tripId))
    }

    fun navigateToPackingMode(tripId: String) {
        if (doc.trips.any { it.id == tripId }) navigateSafely(PacklyRoute.packingMode(tripId))
    }

    LaunchedEffect(initialTripId) {
        initialTripId?.let { navigateSafely(PacklyRoute.packingMode(it)) }
    }

    // Route changes must clear stale screen-scoped actions so nested destinations never inherit a FAB.
    LaunchedEffect(currentRoute) {
        if (routeFabAction?.route != currentRoute) routeFabAction = null
    }

    PacklyScaffold(
        currentRoute = currentRoute,
        canNavigateBack = !isTopLevelRoute,
        nestedTitle = currentRoute.nestedTitle(),
        fabAction = fabAction,
        // Modify Trip persists edits immediately through PacklyAppViewModel, so MVP Save is a commit affordance that returns to the previous route.
        topBarAction = when {
            isTripDetailRoute -> PacklyTopBarAction(
                label = stringResource(R.string.action_save),
                onClick = { navController.popBackStack() },
            )
            currentRoute == PacklyRoute.CreateTripDeadline -> PacklyTopBarAction(
                label = stringResource(R.string.action_skip),
                onClick = {
                    createTripDraftState.clearPackBy()
                    navController.navigate(PacklyRoute.CreateTripLists)
                },
            )
            isTopLevelRoute -> PacklyTopBarAction(
                label = stringResource(R.string.action_options),
                icon = Icons.Rounded.Settings,
                onClick = { navController.navigate(PacklyRoute.Options) },
            )
            else -> null
        },
        useCloseNavigationIcon = isTripDetailRoute || currentRoute == PacklyRoute.CreateTripDetails,
        useTopLevelTitleStyle = currentRoute in createTripRoutes,
        onBack = {
            if (currentRoute == PacklyRoute.CreateTripDetails) {
                createTripDraftState.requestClose { exitCreateTrip() }
            } else {
                navController.popBackStack()
            }
        },
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
            enterTransition = { packlyEnterTransition() },
            exitTransition = { packlyExitTransition() },
            popEnterTransition = { packlyPopEnterTransition() },
            popExitTransition = { packlyPopExitTransition() },
        ) {
            composable(PacklyRoute.Home) {
                HomeScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onStartCreateTrip = { startCreateTrip() },
                    onOpenTrip = { tripId -> navigateToPackingMode(tripId) },
                )
            }
            composable(PacklyRoute.Items) {
                ItemsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { action ->
                        routeFabAction = routeFabAction.updatedForRoute(PacklyRoute.Items, action)
                    },
                    onAdd = vm::addItem,
                    onUpdate = vm::updateItem,
                    onDelete = vm::archiveItem,
                )
            }
            composable(PacklyRoute.Lists) {
                ListsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { action ->
                        routeFabAction = routeFabAction.updatedForRoute(PacklyRoute.Lists, action)
                    },
                    onCreate = vm::createList,
                    onOpen = { navigateSafely(PacklyRoute.listDetail(it)) },
                    onRename = vm::renameList,
                    onDuplicate = { listId -> vm.duplicateList(listId, listCopyNameTemplates) },
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
                    onUseForTrip = { listId ->
                        vm.createTrip(tripFromListDefaultName, "", listId, emptySet())
                        navController.navigate(PacklyRoute.Trips)
                    },
                )
            }
            composable(PacklyRoute.Trips) {
                TripsScreen(
                    doc = doc,
                    contentPadding = shellPadding,
                    onFabActionChange = { action ->
                        routeFabAction = routeFabAction.updatedForRoute(PacklyRoute.Trips, action)
                    },
                    onStartCreateTrip = { startCreateTrip() },
                    onOpen = { navigateToTripDetail(it) },
                    onPack = { navigateToPackingMode(it) },
                    onDelete = vm::deleteTrip,
                )
            }
            composable(PacklyRoute.TripDetail) { backStack ->
                val id = backStack.arguments?.getString("tripId") ?: ""
                TripDetailScreen(
                    doc = doc,
                    tripId = id,
                    contentPadding = shellPadding,
                    onPack = { navigateToPackingMode(id) },
                    onReset = { vm.resetPacking(id) },
                    onQuantityChange = { entryId, quantity -> vm.updateTripEntryQuantity(id, entryId, quantity) },
                    onRemoveEntry = { entryId -> vm.removeTripEntry(id, entryId) },
                    onDeadlineChange = { deadline -> vm.updateTripDeadline(id, deadline) },
                    onToggleSourceList = { sourceListId -> vm.toggleTripSourceList(id, sourceListId) },
                    onToggleSourceItem = { itemId -> vm.toggleTripSourceItem(id, itemId) },
                )
            }
            composable(PacklyRoute.PackingMode) { backStack ->
                val id = backStack.arguments?.getString("tripId") ?: ""
                PackingModeScreen(
                    doc = doc,
                    tripId = id,
                    contentPadding = shellPadding,
                    onSetPacked = { entryId, isPacked -> vm.setPacked(id, entryId, isPacked) },
                )
            }
            composable(PacklyRoute.Options) {
                OptionsScreen(
                    languagePreference = doc.settings.languagePreference,
                    contentPadding = shellPadding,
                    onLanguagePreferenceChange = vm::updateLanguagePreference,
                )
            }
            navigation(
                startDestination = PacklyRoute.CreateTripDetails,
                route = PacklyRoute.CreateTripGraph,
            ) {
                composable(PacklyRoute.CreateTripDetails) {
                    CreateTripDetailsScreen(
                        doc = doc,
                        draftState = createTripDraftState,
                        contentPadding = shellPadding,
                        onNext = { navController.navigate(PacklyRoute.CreateTripDeadline) },
                        onCloseConfirmed = { exitCreateTrip() },
                    )
                }
                composable(PacklyRoute.CreateTripDeadline) {
                    CreateTripDeadlineScreen(
                        draftState = createTripDraftState,
                        contentPadding = shellPadding,
                        onBack = { navController.popBackStack() },
                        onNext = { navController.navigate(PacklyRoute.CreateTripLists) },
                    )
                }
                composable(PacklyRoute.CreateTripLists) {
                    CreateTripListsScreen(
                        doc = doc,
                        draftState = createTripDraftState,
                        contentPadding = shellPadding,
                        onBack = { navController.popBackStack() },
                        onNext = { navController.navigate(PacklyRoute.CreateTripItems) },
                    )
                }
                composable(PacklyRoute.CreateTripItems) {
                    CreateTripItemsScreen(
                        doc = doc,
                        draftState = createTripDraftState,
                        contentPadding = shellPadding,
                        onFabActionChange = { action ->
                            routeFabAction = routeFabAction.updatedForRoute(PacklyRoute.CreateTripItems, action)
                        },
                        onAddItem = { name, categoryId, notes ->
                            vm.addItemForTripDraft(name, categoryId, notes) { itemId ->
                                createTripDraftState.selectItem(itemId)
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onFinish = {
                            vm.createTrip(
                                name = createTripDraftState.name,
                                destination = createTripDraftState.destination,
                                sourceListIds = createTripDraftState.selectedSourceListIds,
                                itemIds = createTripDraftState.selectedItemIds,
                                sourceListEntryIds = createTripDraftState.selectedListEntryIds,
                                itemQuantities = createTripDraftState.itemQuantities,
                                packBy = createTripDraftState.packBy,
                            )
                            createTripDraftState.discard()
                            navController.navigate(PacklyRoute.Trips) {
                                popUpTo(PacklyRoute.CreateTripGraph) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }

}

private data class RouteFabAction(
    val route: String,
    val action: PacklyFabAction,
)

private val createTripRoutes = setOf(
    PacklyRoute.CreateTripDetails,
    PacklyRoute.CreateTripDeadline,
    PacklyRoute.CreateTripLists,
    PacklyRoute.CreateTripItems,
)

private fun RouteFabAction?.updatedForRoute(route: String, action: PacklyFabAction?): RouteFabAction? = when {
    action != null -> RouteFabAction(route, action)
    this?.route == route -> null
    else -> this
}

@Composable
private fun String?.nestedTitle(): String = when (this) {
    PacklyRoute.ListDetail -> stringResource(R.string.nav_lists)
    PacklyRoute.TripDetail -> stringResource(R.string.modify_trip_title)
    PacklyRoute.PackingMode -> stringResource(R.string.app_name)
    PacklyRoute.Options -> stringResource(R.string.options_title)
    in createTripRoutes -> stringResource(R.string.app_name)
    else -> stringResource(R.string.app_name)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.packlyEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(PacklyMotion.navMillis)) +
        slideInHorizontally(animationSpec = tween(PacklyMotion.navMillis)) { it / 12 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.packlyExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(PacklyMotion.navMillis)) +
        slideOutHorizontally(animationSpec = tween(PacklyMotion.navMillis)) { -it / 16 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.packlyPopEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(PacklyMotion.navMillis)) +
        slideInHorizontally(animationSpec = tween(PacklyMotion.navMillis)) { -it / 12 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.packlyPopExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(PacklyMotion.navMillis)) +
        slideOutHorizontally(animationSpec = tween(PacklyMotion.navMillis)) { it / 16 }
