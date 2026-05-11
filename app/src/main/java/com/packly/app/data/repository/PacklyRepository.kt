package com.packly.app.data.repository

import com.packly.app.data.model.Category
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all Packly data operations.
 * Returns Flow&lt;T&gt; for reactive reads — ViewModels collect flows and auto-update.
 * All mutations are suspend functions that serialize through a single Mutex.
 */
interface PacklyRepository {

    // -- Categories --

    fun getCategories(): Flow&lt;List&lt;Category&gt;&gt;

    // -- Items --

    fun getItems(): Flow&lt;List&lt;Item&gt;&gt;
    suspend fun addItem(item: Item)
    suspend fun deleteItem(itemId: String)

    // -- Lists --

    fun getLists(): Flow&lt;List&lt;ItemList&gt;&gt;
    fun getListById(listId: String): Flow&lt;ItemList?&gt;
    suspend fun createList(list: ItemList)
    suspend fun updateList(list: ItemList)
    suspend fun deleteList(listId: String)

    // -- Trips --

    fun getTrips(): Flow&lt;List&lt;Trip&gt;&gt;
    fun getTripById(tripId: String): Flow&lt;Trip?&gt;
    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(tripId: String)

    // -- Bulk --

    suspend fun seedDefaultData()
}
