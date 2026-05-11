package com.packly.app.data.repository

import com.packly.app.data.model.Category
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all Packly data operations.
 * Returns Flow<T> for reactive reads — ViewModels collect flows and auto-update.
 * All mutations are suspend functions that serialize through a single Mutex.
 */
interface PacklyRepository {

    // -- Categories --

    fun getCategories(): Flow<List<Category>>

    // -- Items --

    fun getItems(): Flow<List<Item>>
    suspend fun addItem(item: Item)
    suspend fun deleteItem(itemId: String)

    // -- Lists --

    fun getLists(): Flow<List<ItemList>>
    fun getListById(listId: String): Flow<ItemList?>
    suspend fun createList(list: ItemList)
    suspend fun updateList(list: ItemList)
    suspend fun deleteList(listId: String)

    // -- Trips --

    fun getTrips(): Flow<List<Trip>>
    fun getTripById(tripId: String): Flow<Trip?>
    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(tripId: String)

    // -- Bulk --

    suspend fun seedDefaultData()
}
