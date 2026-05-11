package com.packly.app.data.repository

import android.content.Context
import com.packly.app.data.model.Category
import com.packly.app.data.model.Item
import com.packly.app.data.model.ItemList
import com.packly.app.data.model.PacklyData
import com.packly.app.data.model.Trip
import com.packly.app.data.seed.DefaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON file-backed implementation of PacklyRepository.
 * All writes serialize through a Mutex on Dispatchers.IO.
 * Reads cache in memory via MutableStateFlow for reactive observation.
 */
class JsonPacklyRepository(
    private val context: Context
) : PacklyRepository {

    private val dataFile: File
        get() = File(context.filesDir, "packly_data.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val writeMutex = Mutex()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    private val _lists = MutableStateFlow<List<ItemList>>(emptyList())
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())

    override fun getCategories(): Flow<List<Category>> = _categories.asStateFlow()
    override fun getItems(): Flow<List<Item>> = _items.asStateFlow()
    override fun getLists(): Flow<List<ItemList>> = _lists.asStateFlow()
    override fun getTrips(): Flow<List<Trip>> = _trips.asStateFlow()

    override fun getListById(listId: String): Flow<ItemList?> {
        val list = _lists.value.find { it.id == listId }
        return MutableStateFlow(list).asStateFlow()
    }

    override fun getTripById(tripId: String): Flow<Trip?> {
        val trip = _trips.value.find { it.id == tripId }
        return MutableStateFlow(trip).asStateFlow()
    }

    override suspend fun addItem(item: Item) {
        val current = readData()
        val data = current.copy(items = current.items + item)
        _items.value = data.items
        writeData(data)
    }

    override suspend fun deleteItem(itemId: String) {
        val current = readData()
        val data = current.copy(items = current.items.filter { it.id != itemId })
        _items.value = data.items
        writeData(data)
    }

    override suspend fun createList(list: ItemList) {
        val current = readData()
        val data = current.copy(lists = current.lists + list)
        _lists.value = data.lists
        writeData(data)
    }

    override suspend fun updateList(list: ItemList) {
        val current = readData()
        val data = current.copy(lists = current.lists.map { if (it.id == list.id) list else it })
        _lists.value = data.lists
        writeData(data)
    }

    override suspend fun deleteList(listId: String) {
        val current = readData()
        val data = current.copy(
            lists = current.lists.filter { it.id != listId },
            trips = current.trips.map { if (it.listId == listId) it.copy(listId = null) else it }
        )
        _lists.value = data.lists
        _trips.value = data.trips
        writeData(data)
    }

    override suspend fun createTrip(trip: Trip) {
        val current = readData()
        val data = current.copy(trips = current.trips + trip)
        _trips.value = data.trips
        writeData(data)
    }

    override suspend fun updateTrip(trip: Trip) {
        val current = readData()
        val data = current.copy(trips = current.trips.map { if (it.id == trip.id) trip else it })
        _trips.value = data.trips
        writeData(data)
    }

    override suspend fun deleteTrip(tripId: String) {
        val current = readData()
        val data = current.copy(trips = current.trips.filter { it.id != tripId })
        _trips.value = data.trips
        writeData(data)
    }

    override suspend fun seedDefaultData() {
        val existing = readData()
        if (existing.categories.isNotEmpty() || existing.items.isNotEmpty()) {
            return // Already seeded
        }
        val seeded = existing.copy(
            categories = DefaultData.categories,
            items = DefaultData.items
        )
        _categories.value = seeded.categories
        _items.value = seeded.items
        writeData(seeded)
    }

    // -- Internal helpers --

    private fun readDataFromFile(): PacklyData {
        if (!dataFile.exists()) return PacklyData()
        return json.decodeFromString<PacklyData>(dataFile.readText())
    }

    /**
     * Initialise the repository by reading data from disk.
     * Call this once at app startup.
     */
    suspend fun initialise() {
        val data = readDataFromFile()
        _categories.value = data.categories
        _items.value = data.items
        _lists.value = data.lists
        _trips.value = data.trips
    }

    /**
     * Read data from in-memory state (for mutations).
     */
    private fun readData(): PacklyData = PacklyData(
        version = 1,
        categories = _categories.value,
        items = _items.value,
        lists = _lists.value,
        trips = _trips.value
    )

    /**
     * Write data to disk under Mutex lock.
     */
    private suspend fun writeData(data: PacklyData) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                dataFile.writeText(json.encodeToString(data))
            }
        }
    }
}