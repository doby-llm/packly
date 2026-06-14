package com.dobyllm.packly.feature.trips.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListEntryId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.model.TripStatus

@Stable
class CreateTripDraftState(
    initialName: String = "",
    initialDestination: String = "",
    initialPackBy: InstantString? = null,
    initialSelectedSourceListIds: List<ListId> = emptyList(),
    initialSelectedListEntryIds: Set<ListEntryId> = emptySet(),
    initialSelectedItemIds: Set<ItemId> = emptySet(),
    initialItemQuantities: Map<ItemId, Int> = emptyMap(),
) {
    var name by mutableStateOf(initialName)
        private set

    var destination by mutableStateOf(initialDestination)
        private set

    var packBy by mutableStateOf(initialPackBy)
        private set

    var reminderDraftIncomplete by mutableStateOf(false)
        private set

    var selectedSourceListIds by mutableStateOf(initialSelectedSourceListIds.distinct())
        private set

    var selectedListEntryIds by mutableStateOf(initialSelectedListEntryIds)
        private set

    var selectedItemIds by mutableStateOf(initialSelectedItemIds)
        private set

    var itemQuantities by mutableStateOf(initialItemQuantities.mapValues { (_, quantity) -> quantity.coerceAtLeast(1) })
        private set

    var showDiscardDialog by mutableStateOf(false)
        private set

    val isDirty: Boolean
        get() = name.isNotBlank() || destination.isNotBlank() || packBy != null ||
            selectedSourceListIds.isNotEmpty() || selectedListEntryIds.isNotEmpty() || selectedItemIds.isNotEmpty() || itemQuantities.isNotEmpty() ||
            reminderDraftIncomplete

    fun updateName(value: String) {
        name = value
    }

    fun updateDestination(value: String) {
        destination = value
    }

    fun updatePackBy(value: InstantString?) {
        packBy = value
        if (value != null) reminderDraftIncomplete = false
    }

    fun updateReminderDraftIncomplete(value: Boolean) {
        reminderDraftIncomplete = value
    }

    fun clearPackBy() {
        packBy = null
        reminderDraftIncomplete = false
    }

    fun toggleSourceList(listId: ListId, entries: List<PacklyListEntry>) {
        val entryIds = entries.mapTo(mutableSetOf()) { it.id }
        val allEntriesSelected = entryIds.isNotEmpty() && entryIds.all { it in selectedListEntryIds }
        if (allEntriesSelected) {
            selectedListEntryIds = selectedListEntryIds - entryIds
            selectedSourceListIds = selectedSourceListIds - listId
        } else {
            selectedListEntryIds = selectedListEntryIds + entryIds
            selectedSourceListIds = (selectedSourceListIds + listId).distinct()
        }
    }

    fun toggleSourceListEntry(listId: ListId, entryId: ListEntryId, listEntryIds: Set<ListEntryId>) {
        selectedListEntryIds = if (entryId in selectedListEntryIds) selectedListEntryIds - entryId else selectedListEntryIds + entryId
        selectedSourceListIds = if (selectedListEntryIds.any { it in listEntryIds }) {
            (selectedSourceListIds + listId).distinct()
        } else {
            selectedSourceListIds - listId
        }
    }

    fun removeSourceItems(itemId: ItemId, sourceLists: List<PacklyList>) {
        val sourceEntries = sourceLists.flatMap { it.entries }
        val matchingEntryIds = sourceEntries.mapNotNull { entry -> entry.id.takeIf { entry.itemId == itemId } }.toSet()
        selectedListEntryIds = selectedListEntryIds - matchingEntryIds
        selectedSourceListIds = selectedSourceListIds.filter { listId ->
            sourceLists
                .firstOrNull { it.id == listId }
                ?.entries
                .orEmpty()
                .any { entry -> entry.id in selectedListEntryIds }
        }
        selectedItemIds = selectedItemIds - itemId
        syncQuantitiesFor(selectedItemIds + sourceEntries.mapNotNull { entry -> entry.itemId.takeIf { entry.id in selectedListEntryIds } })
    }

    fun toggleItem(itemId: ItemId) {
        selectedItemIds = if (itemId in selectedItemIds) selectedItemIds - itemId else selectedItemIds + itemId
    }

    fun selectItem(itemId: ItemId) {
        selectedItemIds = selectedItemIds + itemId
        itemQuantities = itemQuantities + (itemId to (itemQuantities[itemId] ?: 1))
    }

    fun setQuantity(itemId: ItemId, quantity: Int) {
        if (itemId !in itemQuantities) return
        itemQuantities = itemQuantities + (itemId to quantity.coerceAtLeast(1))
    }

    fun syncQuantitiesFor(itemIds: Set<ItemId>) {
        itemQuantities = itemQuantities
            .filterKeys { it in itemIds }
            .toMutableMap()
            .apply { itemIds.forEach { putIfAbsent(it, 1) } }
    }

    fun requestClose(onCleanClose: () -> Unit) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onCleanClose()
        }
    }

    fun keepEditing() {
        showDiscardDialog = false
    }

    fun discard() {
        name = ""
        destination = ""
        packBy = null
        reminderDraftIncomplete = false
        selectedSourceListIds = emptyList()
        selectedListEntryIds = emptySet()
        selectedItemIds = emptySet()
        itemQuantities = emptyMap()
        showDiscardDialog = false
    }

    fun duplicateNameIn(doc: PacklyAppDocument): Boolean {
        val trimmedName = name.trim()
        return trimmedName.isNotEmpty() && doc.trips.any { trip ->
            trip.status != TripStatus.Archived && trip.name.equals(trimmedName, ignoreCase = true)
        }
    }

    companion object {
        val Saver: Saver<CreateTripDraftState, List<Any?>> = Saver(
            save = { state ->
                listOf(
                    state.name,
                    state.destination,
                    state.packBy,
                    state.selectedSourceListIds.joinToString("\u0001"),
                    state.selectedListEntryIds.joinToString("\u0001"),
                    state.selectedItemIds.joinToString("\u0001"),
                    state.itemQuantities.entries.joinToString("\u0001") { "${it.key}\u0002${it.value}" },
                )
            },
            restore = { saved ->
                val hasEntrySelectionSlot = saved.size >= 7
                val selectedItemIndex = if (hasEntrySelectionSlot) 5 else 4
                val quantityIndex = if (hasEntrySelectionSlot) 6 else 5
                val quantityMap = saved.getOrNull(quantityIndex)
                    ?.toString()
                    .orEmpty()
                    .split("\u0001")
                    .filter { it.isNotBlank() }
                    .mapNotNull { encoded ->
                        val parts = encoded.split("\u0002", limit = 2)
                        parts.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { key ->
                            key to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
                        }
                    }
                    .toMap()
                CreateTripDraftState(
                    initialName = saved.getOrNull(0)?.toString().orEmpty(),
                    initialDestination = saved.getOrNull(1)?.toString().orEmpty(),
                    initialPackBy = saved.getOrNull(2)?.toString()?.takeIf { it.isNotBlank() },
                    initialSelectedSourceListIds = saved.getOrNull(3)
                        ?.toString()
                        .orEmpty()
                        .split("\u0001")
                        .filter { it.isNotBlank() },
                    initialSelectedListEntryIds = if (hasEntrySelectionSlot) {
                        saved.getOrNull(4)
                            ?.toString()
                            .orEmpty()
                            .split("\u0001")
                            .filter { it.isNotBlank() }
                            .toSet()
                    } else {
                        emptySet()
                    },
                    initialSelectedItemIds = saved.getOrNull(selectedItemIndex)
                        ?.toString()
                        .orEmpty()
                        .split("\u0001")
                        .filter { it.isNotBlank() }
                        .toSet(),
                    initialItemQuantities = quantityMap,
                )
            },
        )
    }
}

@Composable
fun rememberCreateTripDraftState(): CreateTripDraftState = rememberSaveable(saver = CreateTripDraftState.Saver) {
    CreateTripDraftState()
}
