package com.dobyllm.packly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dobyllm.packly.cloud.CloudSyncDeviceIdProvider
import com.dobyllm.packly.cloud.PacklyCloudSyncCoordinator
import com.dobyllm.packly.cloud.PacklyDriveRepositoryFactory
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
import java.util.Locale

class PacklyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataStorePacklyRepository.get(application)
    private val deadlineReminderScheduler = DeadlineReminderScheduler(application)
    private val cloudSyncCoordinator = PacklyCloudSyncCoordinator(
        repository = repository,
        driveRepository = PacklyDriveRepositoryFactory.create(application),
        deviceIdProvider = CloudSyncDeviceIdProvider(application),
    )
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

    fun addItemForTripDraft(name: String, categoryId: CategoryId, notes: String = "", onCreated: (ItemId) -> Unit) = viewModelScope.launch {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return@launch
        val now = PacklyClock.now()
        val newItemId = PacklyIds.item()
        var created = false
        repository.updateDocument { doc ->
            if (doc.items.hasActiveItemName(trimmedName)) {
                doc
            } else {
                created = true
                doc.copy(
                    items = doc.items + PacklyItem(
                        id = newItemId,
                        name = trimmedName,
                        categoryId = categoryId,
                        notes = notes.trim(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
        if (created) onCreated(newItemId)
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

    fun duplicateList(listId: ListId, copyNameTemplates: ListCopyNameTemplates) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateLists { lists ->
            val source = lists.firstOrNull { it.id == listId && !it.isArchived } ?: return@updateLists lists
            lists + source.duplicatedForListAction(
                newListId = PacklyIds.list(),
                now = now,
                existingNames = lists.filterNot { it.isArchived }.map { it.name }.toSet(),
                copyNameTemplates = copyNameTemplates,
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
    ) = createTrip(
        name = name,
        destination = destination,
        sourceListIds = sourceListId?.let { listOf(it) }.orEmpty(),
        itemIds = itemIds,
        itemQuantities = itemQuantities,
        packBy = packBy,
    )

    fun createTrip(
        name: String,
        destination: String,
        sourceListIds: List<ListId>,
        itemIds: Set<ItemId>,
        sourceListEntryIds: Set<ListEntryId>? = null,
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
                val activeSourceListIds = doc.activeSourceListIdsInSelectedOrder(sourceListIds)
                val entries = doc.buildTripEntries(
                    sourceListIds = activeSourceListIds,
                    itemIds = itemIds,
                    sourceListEntryIds = sourceListEntryIds,
                    itemQuantities = itemQuantities,
                )
                doc.copy(
                    trips = doc.trips + PacklyTrip(
                        id = PacklyIds.trip(),
                        name = trimmedName,
                        destination = destination.trim(),
                        sourceListId = activeSourceListIds.singleOrNull(),
                        packBy = packBy,
                        entries = entries,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    fun addTripEntries(
        tripId: TripId,
        sourceListIds: List<ListId>,
        itemIds: Set<ItemId>,
        itemQuantities: Map<ItemId, Int> = emptyMap(),
    ) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            val activeSourceListIds = doc.activeSourceListIdsInSelectedOrder(sourceListIds)
            doc.copy(
                trips = doc.trips.map { trip ->
                    if (trip.id != tripId) {
                        trip
                    } else {
                        val nextSortOrder = (trip.entries.maxOfOrNull { it.sortOrder } ?: -1) + 1
                        val newEntryCandidates = doc.buildTripEntries(
                            sourceListIds = activeSourceListIds,
                            itemIds = itemIds,
                            itemQuantities = itemQuantities,
                            sortOrderOffset = nextSortOrder,
                        )

                        trip.withAdditionalEntriesForTripAction(newEntryCandidates, now, doc.categories)
                    }
                },
            )
        }
    }

    fun toggleTripSourceList(tripId: TripId, sourceListId: ListId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            val sourceList = doc.lists.firstOrNull { it.id == sourceListId && !it.isArchived } ?: return@updateDocument doc
            val newEntryCandidates = doc.buildTripEntries(
                sourceListIds = listOf(sourceListId),
                itemIds = emptySet(),
                itemQuantities = emptyMap(),
            )

            doc.copy(
                trips = doc.trips.map { trip ->
                    if (trip.id == tripId) {
                        trip.withToggledSourceListForTripAction(sourceList, newEntryCandidates, now, doc.categories)
                    } else {
                        trip
                    }
                },
            )
        }
    }

    fun toggleTripSourceItem(tripId: TripId, itemId: ItemId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            val item = doc.items.firstOrNull { it.id == itemId && !it.isArchived } ?: return@updateDocument doc
            val newEntryCandidates = doc.buildTripEntries(
                sourceListIds = emptyList(),
                itemIds = setOf(itemId),
                itemQuantities = emptyMap(),
            )

            doc.copy(
                trips = doc.trips.map { trip ->
                    if (trip.id == tripId) {
                        trip.withToggledSourceItemForTripAction(item, newEntryCandidates, now, doc.categories)
                    } else {
                        trip
                    }
                },
            )
        }
    }

    fun updateTripContents(
        tripId: TripId,
        sourceListIds: List<ListId>,
        sourceListEntryIds: Set<ListEntryId>,
        itemIds: Set<ItemId>,
        itemQuantities: Map<ItemId, Int>,
    ) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateDocument { doc ->
            val rebuiltEntries = doc.buildTripEntries(
                sourceListIds = doc.activeSourceListIdsInSelectedOrder(sourceListIds),
                itemIds = itemIds,
                sourceListEntryIds = sourceListEntryIds,
                itemQuantities = itemQuantities,
            )

            doc.copy(
                trips = doc.trips.map { trip ->
                    if (trip.id == tripId) trip.withReplacedEntriesForTripAction(rebuiltEntries, now) else trip
                },
            )
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
            if (trip.id != tripId) trip else trip.withUpdatedEntryQuantityForTripAction(entryId, quantity, now)
        } }
    }

    fun removeTripEntry(tripId: TripId, entryId: TripEntryId) = viewModelScope.launch {
        val now = PacklyClock.now()
        repository.updateTrips { trips -> trips.map { trip ->
            if (trip.id != tripId) trip else trip.withRemovedEntryForTripAction(entryId, now)
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

    fun updateLanguagePreference(languagePreference: LanguagePreference) = viewModelScope.launch {
        repository.updateSettings { settings -> settings.copy(languagePreference = languagePreference) }
    }

    fun syncWithGoogleDrive() = viewModelScope.launch { cloudSyncCoordinator.syncNow() }
}

private data class TripEntryDraft(
    val sourceItemId: ItemId?,
    val sourceListEntryId: ListEntryId?,
    val nameSnapshot: String,
    val categoryIdSnapshot: CategoryId,
    val notes: String,
)

private fun PacklyAppDocument.activeSourceListIdsInSelectedOrder(sourceListIds: List<ListId>): List<ListId> {
    val activeListIds = lists.filterNot { it.isArchived }.mapTo(mutableSetOf()) { it.id }
    return sourceListIds.distinct().filter { it in activeListIds }
}

internal fun PacklyAppDocument.buildTripEntries(
    sourceListIds: List<ListId>,
    itemIds: Set<ItemId>,
    sourceListEntryIds: Set<ListEntryId>? = null,
    itemQuantities: Map<ItemId, Int>,
    sortOrderOffset: Int = 0,
): List<TripEntry> {
    val listsById = lists.filterNot { it.isArchived }.associateBy { it.id }
    val activeItemsById = items.filterNot { it.isArchived }.associateBy { it.id }
    val seenItemIds = mutableSetOf<ItemId>()
    val seenSnapshotKeys = mutableSetOf<String>()
    val seenNoItemSnapshotKeys = mutableSetOf<String>()
    val drafts = mutableListOf<TripEntryDraft>()

    fun addDraft(draft: TripEntryDraft) {
        val sourceItemId = draft.sourceItemId
        val snapshotKey = snapshotDedupeKey(draft.nameSnapshot, draft.categoryIdSnapshot)
        val isDuplicate = if (sourceItemId != null) {
            sourceItemId in seenItemIds || snapshotKey in seenNoItemSnapshotKeys
        } else {
            snapshotKey in seenSnapshotKeys
        }
        if (!isDuplicate) {
            if (sourceItemId != null) {
                seenItemIds += sourceItemId
            } else {
                seenNoItemSnapshotKeys += snapshotKey
            }
            seenSnapshotKeys += snapshotKey
            drafts += draft
        }
    }

    sourceListIds.distinct().forEach { listId ->
        listsById[listId]
            ?.entries
            .orEmpty()
            .sortedBy { it.sortOrder }
            .filter { entry -> sourceListEntryIds == null || entry.id in sourceListEntryIds }
            .filter { entry -> entry.itemId == null || entry.itemId in activeItemsById }
            .forEach { entry ->
                addDraft(
                    TripEntryDraft(
                        sourceItemId = entry.itemId,
                        sourceListEntryId = entry.id,
                        nameSnapshot = entry.itemNameSnapshot,
                        categoryIdSnapshot = entry.categoryIdSnapshot,
                        notes = entry.notes,
                    ),
                )
            }
    }

    items.filter { item -> item.id in itemIds && !item.isArchived }
        .forEach { item ->
            addDraft(
                TripEntryDraft(
                    sourceItemId = item.id,
                    sourceListEntryId = null,
                    nameSnapshot = item.name,
                    categoryIdSnapshot = item.categoryId,
                    notes = item.notes,
                ),
            )
        }

    return drafts.mapIndexed { index, draft ->
        TripEntry(
            id = PacklyIds.tripEntry(),
            sourceItemId = draft.sourceItemId,
            sourceListEntryId = draft.sourceListEntryId,
            nameSnapshot = draft.nameSnapshot,
            categoryIdSnapshot = draft.categoryIdSnapshot,
            quantity = draft.sourceItemId?.let { itemQuantities[it] }?.coerceAtLeast(1) ?: 1,
            notes = draft.notes,
            sortOrder = sortOrderOffset + index,
        )
    }
}

internal fun PacklyTrip.withAdditionalEntriesForTripAction(
    newEntryCandidates: List<TripEntry>,
    now: InstantString,
    categories: List<PacklyCategory> = emptyList(),
): PacklyTrip {
    val existingItemIds = entries.mapNotNull { it.sourceItemId }.toMutableSet()
    val existingSnapshotKeys = entries.mapTo(mutableSetOf()) { it.snapshotDedupeKey() }
    val existingNoItemSnapshotKeys = entries
        .filter { it.sourceItemId == null }
        .mapTo(mutableSetOf()) { it.snapshotDedupeKey() }
    val nextSortOrder = (entries.maxOfOrNull { it.sortOrder } ?: -1) + 1
    val newEntries = newEntryCandidates.filter { entry ->
        val sourceItemId = entry.sourceItemId
        val snapshotKey = entry.snapshotDedupeKey()
        val isDuplicate = if (sourceItemId != null) {
            sourceItemId in existingItemIds || snapshotKey in existingNoItemSnapshotKeys
        } else {
            snapshotKey in existingSnapshotKeys
        }
        if (isDuplicate) {
            false
        } else {
            if (sourceItemId != null) {
                existingItemIds += sourceItemId
            } else {
                existingNoItemSnapshotKeys += snapshotKey
            }
            existingSnapshotKeys += snapshotKey
            true
        }
    }.mapIndexed { index, entry ->
        entry.copy(sortOrder = nextSortOrder + index)
    }

    return if (newEntries.isEmpty()) {
        this
    } else {
        copy(entries = (entries + newEntries).groupedForTripReview(categories), updatedAt = now)
    }
}

internal fun PacklyTrip.withToggledSourceListForTripAction(
    sourceList: PacklyList,
    newEntryCandidates: List<TripEntry>,
    now: InstantString,
    categories: List<PacklyCategory> = emptyList(),
): PacklyTrip {
    val sourceEntryKeys = sourceList.entries.mapTo(mutableSetOf()) { it.tripMembershipKey() }
    if (sourceEntryKeys.isEmpty()) return this

    val allSourceEntriesPresent = sourceEntryKeys.all { sourceKey ->
        entries.any { entry -> entry.matchesTripMembership(sourceKey) }
    }

    return if (allSourceEntriesPresent) {
        withoutEntriesMatching(now) { entry -> entry.matchesAnyTripMembership(sourceEntryKeys) }
    } else {
        withAdditionalEntriesForTripAction(newEntryCandidates, now, categories)
    }
}

internal fun PacklyTrip.withToggledSourceItemForTripAction(
    sourceItem: PacklyItem,
    newEntryCandidates: List<TripEntry>,
    now: InstantString,
    categories: List<PacklyCategory> = emptyList(),
): PacklyTrip {
    val sourceKey = sourceItem.tripMembershipKey()
    return if (entries.any { it.matchesTripMembership(sourceKey) }) {
        withoutEntriesMatching(now) { entry -> entry.matchesTripMembership(sourceKey) }
    } else {
        withAdditionalEntriesForTripAction(newEntryCandidates, now, categories)
    }
}

internal fun PacklyTrip.withUpdatedEntryQuantityForTripAction(
    entryId: TripEntryId,
    quantity: Int,
    now: InstantString,
): PacklyTrip = copy(
    entries = entries.map { entry ->
        if (entry.id == entryId) entry.copy(quantity = quantity.coerceAtLeast(1)) else entry
    },
    updatedAt = now,
)

internal fun PacklyTrip.withReplacedEntriesForTripAction(newEntries: List<TripEntry>, now: InstantString): PacklyTrip {
    val previousEntriesByKey = entries.associateBy { it.tripContentEditKey() }
    val updatedEntries = newEntries.mapIndexed { index, entry ->
        val previous = previousEntriesByKey[entry.tripContentEditKey()]
        entry.copy(
            id = previous?.id ?: entry.id,
            isPacked = previous?.isPacked ?: false,
            packedAt = previous?.packedAt,
            sortOrder = index,
        )
    }

    return if (updatedEntries == entries) this else copy(entries = updatedEntries, updatedAt = now)
}

internal fun PacklyTrip.withRemovedEntryForTripAction(entryId: TripEntryId, now: InstantString): PacklyTrip {
    var removed = false
    val updated = withoutEntriesMatching(now) { entry ->
        if (!removed && entry.id == entryId) {
            removed = true
            true
        } else {
            false
        }
    }

    return if (removed) updated else this
}

private fun PacklyTrip.withoutEntriesMatching(now: InstantString, shouldRemove: (TripEntry) -> Boolean): PacklyTrip {
    var removed = false
    val remainingEntries = entries.filterNot { entry ->
        shouldRemove(entry).also { matched -> if (matched) removed = true }
    }

    if (!removed) return this

    return copy(
        entries = remainingEntries
            .sortedWith(compareBy<TripEntry> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { index, entry -> entry.copy(sortOrder = index) },
        updatedAt = now,
    )
}

private data class TripCategorySortKey(
    val sortOrder: Int,
    val label: String,
    val id: CategoryId,
)

private fun List<TripEntry>.groupedForTripReview(categories: List<PacklyCategory>): List<TripEntry> {
    if (categories.isEmpty()) {
        return sortedWith(compareBy<TripEntry> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { index, entry -> entry.copy(sortOrder = index) }
    }

    val categorySortKeys = categories
        .filterNot { it.isArchived }
        .associate { category ->
            category.id to TripCategorySortKey(
                sortOrder = category.sortOrder,
                label = category.label.normalizedForSort(),
                id = category.id,
            )
        }

    return sortedWith(
        compareBy<TripEntry> { entry -> entry.categorySortKey(categorySortKeys).sortOrder }
            .thenBy { entry -> entry.categorySortKey(categorySortKeys).label }
            .thenBy { entry -> entry.categorySortKey(categorySortKeys).id }
            .thenBy { entry -> entry.sortOrder }
            .thenBy { entry -> entry.id },
    ).mapIndexed { index, entry -> entry.copy(sortOrder = index) }
}

private fun TripEntry.categorySortKey(categorySortKeys: Map<CategoryId, TripCategorySortKey>): TripCategorySortKey =
    categorySortKeys[categoryIdSnapshot] ?: TripCategorySortKey(
        sortOrder = Int.MAX_VALUE,
        label = categoryIdSnapshot.normalizedForSort(),
        id = categoryIdSnapshot,
    )

private fun TripEntry.snapshotDedupeKey(): String = snapshotDedupeKey(nameSnapshot, categoryIdSnapshot)

private fun TripEntry.tripContentEditKey(): String = when {
    sourceListEntryId != null -> "list-entry:$sourceListEntryId"
    sourceItemId != null -> "item:$sourceItemId"
    else -> snapshotDedupeKey()
}

private data class TripMembershipKey(
    val sourceItemId: ItemId?,
    val snapshotKey: String,
)

private fun PacklyListEntry.tripMembershipKey(): TripMembershipKey =
    TripMembershipKey(itemId, snapshotDedupeKey(itemNameSnapshot, categoryIdSnapshot))

private fun PacklyItem.tripMembershipKey(): TripMembershipKey =
    TripMembershipKey(id, snapshotDedupeKey(name, categoryId))

private fun TripEntry.matchesAnyTripMembership(sourceKeys: Set<TripMembershipKey>): Boolean =
    sourceKeys.any(::matchesTripMembership)

private fun TripEntry.matchesTripMembership(sourceKey: TripMembershipKey): Boolean =
    sourceItemId?.let { it == sourceKey.sourceItemId } ?: (snapshotDedupeKey() == sourceKey.snapshotKey)

private fun snapshotDedupeKey(name: String, categoryId: CategoryId): String =
    "snapshot:${name.normalizedForDedupe()}\u0000$categoryId"

private fun String.normalizedForDedupe(): String = trim().lowercase().replace(Regex("\\s+"), " ")

private fun String.normalizedForSort(): String = normalizedForDedupe()

internal fun PacklyList.renamedForListAction(name: String, now: InstantString): PacklyList =
    copy(name = name.trim(), updatedAt = now)

internal fun PacklyList.duplicatedForListAction(
    newListId: ListId,
    now: InstantString,
    existingNames: Set<String>,
    copyNameTemplates: ListCopyNameTemplates,
    newEntryId: () -> ListEntryId = PacklyIds::listEntry,
): PacklyList = copy(
    id = newListId,
    name = nextListCopyName(name, existingNames, copyNameTemplates),
    entries = entries.map { it.copy(id = newEntryId()) },
    isSeed = false,
    isArchived = false,
    createdAt = now,
    updatedAt = now,
)

data class ListCopyNameTemplates(
    private val unnumberedTemplate: String,
    private val numberedTemplate: String,
) {
    fun unnumbered(baseName: String): String = unnumberedTemplate.formatLocalized(baseName)

    fun numbered(baseName: String, suffix: Int): String = numberedTemplate.formatLocalized(baseName, suffix)
}

private fun String.formatLocalized(vararg args: Any): String = String.format(Locale.ROOT, this, *args)

private fun nextListCopyName(
    baseName: String,
    existingNames: Set<String>,
    copyNameTemplates: ListCopyNameTemplates,
): String {
    val normalizedNames = existingNames.map { it.lowercase() }.toSet()
    val copyName = copyNameTemplates.unnumbered(baseName)
    if (copyName.lowercase() !in normalizedNames) return copyName

    var suffix = 2
    while (copyNameTemplates.numbered(baseName, suffix).lowercase() in normalizedNames) suffix += 1
    return copyNameTemplates.numbered(baseName, suffix)
}

private fun Iterable<PacklyItem>.hasActiveItemName(name: String, exceptId: ItemId? = null): Boolean =
    any { item -> !item.isArchived && item.id != exceptId && item.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyList>.hasActiveListName(name: String, exceptId: ListId? = null): Boolean =
    any { list -> !list.isArchived && list.id != exceptId && list.name.equals(name, ignoreCase = true) }

private fun Iterable<PacklyTrip>.hasActiveTripName(name: String): Boolean =
    any { trip -> trip.status != TripStatus.Archived && trip.name.equals(name, ignoreCase = true) }
