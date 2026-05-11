package com.packly.app.di

import android.content.Context
import com.packly.app.data.repository.JsonPacklyRepository
import com.packly.app.data.repository.PacklyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency injection module for Packly.
 * Single source of truth for all dependencies.
 * Lazy singleton pattern — repository is created once and shared.
 */
class AppModule(
    private val context: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val repository: PacklyRepository by lazy {
        JsonPacklyRepository(context.applicationContext).also { repo ->
            scope.launch { repo.initialise() }
        }
    }

    /**
     * Seed default data on first launch.
     * Called from PacklyApplication.onCreate().
     */
    fun seedOnFirstLaunch() {
        scope.launch {
            repository.seedDefaultData()
        }
    }

    // ViewModel factories will be added in later phases:
    // fun provideHomeViewModel(): HomeViewModel = ...
    // fun provideItemsViewModel(): ItemsViewModel = ...
    // fun provideListsViewModel(): ListsViewModel = ...
    // fun provideTripsViewModel(): TripsViewModel = ...

    // NavController placeholder for Phase 2
    // fun provideNavController(): NavHostController = ...
}