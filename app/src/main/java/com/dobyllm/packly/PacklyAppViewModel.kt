package com.dobyllm.packly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.core.time.PacklyClock
import com.dobyllm.packly.core.time.PacklyIds
import com.dobyllm.packly.data.repository.DataStorePacklyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PacklyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataStorePacklyRepository.get(application)
    val document: StateFlow<PacklyAppDocument> = repository.appState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), com.dobyllm.packly.data.seed.SeedDataProvider.initialDocument()
    )

    fun addItem(name: String, categoryId: CategoryId, quantity: Int, notes: String = "") = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateItems { items ->
            if (items.hasActiveItemName(trimmedName)) {
                items
            } else {
                items + PacklyItem(
                    PacklyIds.item(),
                    trimmedName,
                    categoryId,
                    quantity.coerceAtLeast(1),
                    notes.trim(),
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
    }

    fun updateItem(itemId: ItemId, name: String, categoryId: CategoryId, quantity: Int, notes: String = "") = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateItems { items ->
            if (items.hasActiveItemName(trimmedName, exceptId = itemId)) {
                items
            } else {
                items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            name = trimmedName,
                            categoryId = categoryId,
                            defaultQuantity = quantity.coerceAtLeast(1),
                            notes = notes.trim(),
                            updatedAt = now,
                        )
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun archiveItem(itemId: ItemId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateItems { items -> items.map { if (it.id == itemId) it.copy(isArchived = true, updatedAt = now) else it } }
    }

    fun createList(name: String, description: String = "", selectedItemIds: Set<ItemId> = emptySet()) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            if (doc.lists.hasActiveListName(trimmedName)) {
                doc
            } else {
                val selected = doc.items.filter { it.id in selectedItemIds && !it.isArchived }
                val entries = selected.mapIndexed { index, item ->
                    PacklyListEntry(PacklyIds.listEntry(), item.id, item.name, item.categoryId, item.defaultQuantity, item.notes, index)
                }
                doc.copy(lists = doc.lists + PacklyList(PacklyIds.list(), trimmedName, description.trim(), entries, createdAt = now, updatedAt = now))
            }
        }
    }

    fun deleteList(listId: ListId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateLists { lists -> lists.map { if (it.id == listId) it.copy(isArchived = true, updatedAt = now) else it } }
    }

    fun toggleListItem(listId: ListId, itemId: ItemId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            val item = doc.items.firstOrNull { it.id == itemId } ?: return@updateDocument doc
            doc.copy(lists = doc.lists.map { list ->
                if (list.id != listId) list else {
                    val exists = list.entries.any { it.itemId == itemId }
                    val entries = if (exists) list.entries.filterNot { it.itemId == itemId } else list.entries + PacklyListEntry(PacklyIds.listEntry(), item.id, item.name, item.categoryId, item.defaultQuantity, item.notes, list.entries.size)
                    list.copy(entries = entries.mapIndexed { index, entry -> entry.copy(sortOrder = index) }, updatedAt = now)
                }
            })
        }
    }

    fun createTrip(name: String, destination: String, sourceListId: ListId?, itemIds: Set<ItemId>) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            if (doc.trips.hasActiveTripName(trimmedName)) {
                doc
            } else {
                val fromList = sourceListId?.let { doc.lists.firstOrNull { list -> list.id == it } }?.entries ?: emptyList()
                val fromItems = doc.items.filter { it.id in itemIds && fromList.none { entry -> entry.itemId == it.id } }
                    .mapIndexed { index, item -> PacklyListEntry(PacklyIds.listEntry(), item.id, item.name, item.categoryId, item.defaultQuantity, item.notes, fromList.size + index) }
                val entries = (fromList + fromItems).distinctBy { it.itemId ?: it.itemNameSnapshot.lowercase() }.mapIndexed { index, entry ->
                    TripEntry(PacklyIds.tripEntry(), entry.itemId, entry.id, entry.itemNameSnapshot, entry.categoryIdSnapshot, entry.quantity, entry.notes, sortOrder = index)
                }
                doc.copy(trips = doc.trips + PacklyTrip(PacklyIds.trip(), trimmedName, destination.trim(), sourceListId = sourceListId, entries = entries, createdAt = now, updatedAt = now))
            }
        }
    }

    fun deleteTrip(tripId: TripId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { if (it.id == tripId) it.copy(status = TripStatus.Archived, updatedAt = now) else it } }
    }

    fun togglePacked(tripId: TripId, entryId: TripEntryId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { trip ->
            if (trip.id != tripId) trip else trip.copy(
                entries = trip.entries.map { entry -> if (entry.id == entryId) entry.copy(isPacked = !entry.isPacked, packedAt = if (!entry.isPacked) now else null) else entry },
                updatedAt = now,
            )
        } }
    }

    fun resetPacking(tripId: TripId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { if (it.id == tripId) it.copy(entries = it.entries.map { e -> e.copy(isPacked = false, packedAt = null) }, updatedAt = now) else it } }
    }
}

private fun Iterable<PacklyItem>.hasActiveItemName(name: String, exceptId: ItemId? = null): Boolean =
    any { item -> !item.isArchived && item.id != exceptId && item.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyList>.hasActiveListName(name: String): Boolean =
    any { list -> !list.isArchived && list.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyTrip>.hasActiveTripName(name: String): Boolean =
    any { trip -> trip.status != TripStatus.Archived && trip.name.equals(name, ignoreCase = true) }
