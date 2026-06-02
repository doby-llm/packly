package com.dobyllm.packly.data.repository

import android.content.Context
import androidx.datastore.dataStore
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.data.json.PacklySerializer
import com.dobyllm.packly.domain.repository.PacklyRepository
import kotlinx.coroutines.flow.Flow

private val Context.packlyDataStore by dataStore("packly.json", PacklySerializer)

class DataStorePacklyRepository private constructor(context: Context) : PacklyRepository {
    private val dataStore = context.applicationContext.packlyDataStore
    override val appState: Flow<PacklyAppDocument> = dataStore.data

    override suspend fun updateDocument(transform: (PacklyAppDocument) -> PacklyAppDocument) {
        dataStore.updateData { current -> transform(current.ensureSeeded()) }
    }
    override suspend fun updateItems(transform: (List<PacklyItem>) -> List<PacklyItem>) = updateDocument { it.copy(items = transform(it.items)) }
    override suspend fun updateLists(transform: (List<PacklyList>) -> List<PacklyList>) = updateDocument { it.copy(lists = transform(it.lists)) }
    override suspend fun updateTrips(transform: (List<PacklyTrip>) -> List<PacklyTrip>) = updateDocument { it.copy(trips = transform(it.trips)) }
    override suspend fun updateSettings(transform: (PacklySettings) -> PacklySettings) = updateDocument { it.copy(settings = transform(it.settings)) }

    private fun PacklyAppDocument.ensureSeeded(): PacklyAppDocument =
        if (categories.isEmpty() && items.isEmpty() && lists.isEmpty()) com.dobyllm.packly.data.seed.SeedDataProvider.initialDocument() else this

    companion object {
        @Volatile private var instance: DataStorePacklyRepository? = null
        fun get(context: Context): DataStorePacklyRepository = instance ?: synchronized(this) {
            instance ?: DataStorePacklyRepository(context).also { instance = it }
        }
    }
}
