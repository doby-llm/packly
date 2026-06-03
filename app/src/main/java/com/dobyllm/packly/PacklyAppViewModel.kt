package com.dobyllm.packly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.core.time.PacklyClock
import com.dobyllm.packly.core.time.PacklyIds
import com.dobyllm.packly.data.repository.DataStorePacklyRepository
import com.dobyllm.packly.notification.DeadlineReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PacklyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataStorePacklyRepository.get(application)
    private val deadlineReminderScheduler = DeadlineReminderScheduler(application)
    val document: StateFlow<PacklyAppDocument> = repository.appState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), com.dobyllm.packly.data.seed.SeedDataProvider.initialDocument()
    )

    init {
        viewModelScope.launch {
            document.collectLatest { doc -> deadlineReminderScheduler.syncAll(doc.trips) }
        }
    }

    fun addItem(name: String, categoryId: CategoryId, notes: String = "") = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateItems { items ->
            if (items.hasActiveItemName(trimmedName)) {
                items
            } else {
                items + PacklyItem(
                    id = PacklyIds.item(),
                    name = trimmedName,
                    categoryId = categoryId,
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
    }

    fun updateItem(itemId: ItemId, name: String, categoryId: CategoryId, notes: String = "") = viewModelScope.launch {
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
                    PacklyListEntry(
                        id = PacklyIds.listEntry(),
                        itemId = item.id,
                        itemNameSnapshot = item.name,
                        categoryIdSnapshot = item.categoryId,
                        notes = item.notes,
                        sortOrder = index,
                    )
                }
                doc.copy(lists = doc.lists + PacklyList(PacklyIds.list(), trimmedName, description.trim(), entries, createdAt = now, updatedAt = now))
            }
        }
    }

    fun renameList(listId: ListId, name: String) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateLists { lists ->
            if (lists.hasActiveListName(trimmedName, exceptId = listId)) {
                lists
            } else {
                lists.map { list ->
                    if (list.id == listId) list.renamedForListAction(trimmedName, now) else list
                }
            }
        }
    }

    fun duplicateList(listId: ListId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateLists { lists ->
            val source = lists.firstOrNull { it.id == listId && !it.isArchived } ?: return@updateLists lists
            lists + source.duplicatedForListAction(
                newListId = PacklyIds.list(),
                now = now,
                existingNames = lists.filterNot { it.isArchived }.map { it.name }.toSet(),
            )
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
                    val newEntry = PacklyListEntry(
                        id = PacklyIds.listEntry(),
                        itemId = item.id,
                        itemNameSnapshot = item.name,
                        categoryIdSnapshot = item.categoryId,
                        notes = item.notes,
                        sortOrder = list.entries.size,
                    )
                    val entries = if (exists) list.entries.filterNot { it.itemId == itemId } else list.entries + newEntry
                    list.copy(entries = entries.mapIndexed { index, entry -> entry.copy(sortOrder = index) }, updatedAt = now)
                }
            })
        }
    }

    fun createTrip(
        name: String,
        destination: String,
        sourceListId: ListId?,
        itemIds: Set<ItemId>,
        itemQuantities: Map<ItemId, Int> = emptyMap(),
        packBy: InstantString? = null,
    ) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            if (doc.trips.hasActiveTripName(trimmedName)) {
                doc
            } else {
                val fromList = sourceListId?.let { doc.lists.firstOrNull { list -> list.id == it } }?.entries ?: emptyList()
                val fromItems = doc.items.filter { it.id in itemIds && fromList.none { entry -> entry.itemId == it.id } }
                    .mapIndexed { index, item ->
                        PacklyListEntry(
                            id = PacklyIds.listEntry(),
                            itemId = item.id,
                            itemNameSnapshot = item.name,
                            categoryIdSnapshot = item.categoryId,
                            notes = item.notes,
                            sortOrder = fromList.size + index,
                        )
                    }
                val entries = (fromList + fromItems).distinctBy { it.itemId ?: it.itemNameSnapshot.lowercase() }.mapIndexed { index, entry ->
                    TripEntry(
                        id = PacklyIds.tripEntry(),
                        sourceItemId = entry.itemId,
                        sourceListEntryId = entry.id,
                        nameSnapshot = entry.itemNameSnapshot,
                        categoryIdSnapshot = entry.categoryIdSnapshot,
                        quantity = entry.itemId?.let { itemQuantities[it] }?.coerceAtLeast(1) ?: 1,
                        notes = entry.notes,
                        sortOrder = index,
                    )
                }
                doc.copy(trips = doc.trips + PacklyTrip(PacklyIds.trip(), trimmedName, destination.trim(), sourceListId = sourceListId, packBy = packBy, entries = entries, createdAt = now, updatedAt = now))
            }
        }
    }

    fun updateTripDeadline(tripId: TripId, packBy: InstantString?) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips ->
            trips.map { trip ->
                if (trip.id == tripId) trip.copy(packBy = packBy, updatedAt = now) else trip
            }
        }
    }

    fun updateTripEntryQuantity(tripId: TripId, entryId: TripEntryId, quantity: Int) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { trip ->
            if (trip.id != tripId) trip else trip.copy(
                entries = trip.entries.map { entry ->
                    if (entry.id == entryId) entry.copy(quantity = quantity.coerceAtLeast(1)) else entry
                },
                updatedAt = now,
            )
        } }
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

    fun setPacked(tripId: TripId, entryId: TripEntryId, isPacked: Boolean) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { trip ->
            if (trip.id != tripId) trip else trip.copy(
                entries = trip.entries.map { entry ->
                    if (entry.id == entryId) {
                        entry.copy(isPacked = isPacked, packedAt = if (isPacked) now else null)
                    } else {
                        entry
                    }
                },
                updatedAt = now,
            )
        } }
    }

    fun resetPacking(tripId: TripId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { if (it.id == tripId) it.copy(entries = it.entries.map { e -> e.copy(isPacked = false, packedAt = null) }, updatedAt = now) else it } }
    }

    fun updateThemeMode(themeMode: ThemeMode) = viewModelScope.launch {
        repository.updateSettings { settings -> settings.copy(themeMode = themeMode) }
    }
}

internal fun PacklyList.renamedForListAction(name: String, now: InstantString): PacklyList =
    copy(name = name.trim(), updatedAt = now)

internal fun PacklyList.duplicatedForListAction(
    newListId: ListId,
    now: InstantString,
    existingNames: Set<String>,
    newEntryId: () -> ListEntryId = PacklyIds::listEntry,
): PacklyList = copy(
    id = newListId,
    name = nextListCopyName(name, existingNames),
    entries = entries.map { it.copy(id = newEntryId()) },
    isSeed = false,
    isArchived = false,
    createdAt = now,
    updatedAt = now,
)

private fun nextListCopyName(baseName: String, existingNames: Set<String>): String {
    val normalizedNames = existingNames.map { it.lowercase() }.toSet()
    val copyName = "$baseName copy"
    if (copyName.lowercase() !in normalizedNames) return copyName

    var suffix = 2
    while ("$copyName $suffix".lowercase() in normalizedNames) suffix += 1
    return "$copyName $suffix"
}

private fun Iterable<PacklyItem>.hasActiveItemName(name: String, exceptId: ItemId? = null): Boolean =
    any { item -> !item.isArchived && item.id != exceptId && item.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyList>.hasActiveListName(name: String, exceptId: ListId? = null): Boolean =
    any { list -> !list.isArchived && list.id != exceptId && list.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyTrip>.hasActiveTripName(name: String): Boolean =
    any { trip -> trip.status != TripStatus.Archived && trip.name.equals(name, ignoreCase = true) }
