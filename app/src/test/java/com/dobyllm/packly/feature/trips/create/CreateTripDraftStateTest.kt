package com.dobyllm.packly.feature.trips.create

import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CreateTripDraftStateTest {
    @Test
    fun setQuantityUpdatesSyncedListDerivedItems() {
        val draftState = CreateTripDraftState(initialSelectedItemIds = emptySet())

        draftState.syncQuantitiesFor(setOf("item_from_list"))
        draftState.setQuantity("item_from_list", 3)

        assertEquals(3, draftState.itemQuantities["item_from_list"])
    }

    @Test
    fun setQuantityStillUpdatesStandaloneItemsAndCoercesToOne() {
        val draftState = CreateTripDraftState(initialSelectedItemIds = setOf("item_standalone"))

        draftState.syncQuantitiesFor(draftState.selectedItemIds)
        draftState.setQuantity("item_standalone", 0)

        assertEquals(1, draftState.itemQuantities["item_standalone"])
    }

    @Test
    fun setQuantityIgnoresItemsThatAreNotPartOfTheReviewSet() {
        val draftState = CreateTripDraftState(initialSelectedItemIds = setOf("item_selected"))

        draftState.syncQuantitiesFor(draftState.selectedItemIds)
        draftState.setQuantity("item_not_in_review", 5)

        assertFalse(draftState.itemQuantities.containsKey("item_not_in_review"))
    }

    @Test
    fun togglingStandaloneItemKeepsSyncedListDerivedQuantitiesUntilFullSyncRuns() {
        val draftState = CreateTripDraftState(initialSelectedItemIds = emptySet())

        draftState.syncQuantitiesFor(setOf("item_from_list"))
        draftState.setQuantity("item_from_list", 4)
        draftState.toggleItem("item_standalone")

        assertEquals(4, draftState.itemQuantities["item_from_list"])
    }

    @Test
    fun removeSourceItemsOnlyDropsSourceListsWithoutRemainingSelectedEntries() {
        val listA = packlyList(
            id = "list_a",
            entries = listOf(
                packlyListEntry(id = "entry_a_shirt", itemId = "item_shirt"),
                packlyListEntry(id = "entry_a_socks", itemId = "item_socks"),
            ),
        )
        val listB = packlyList(
            id = "list_b",
            entries = listOf(packlyListEntry(id = "entry_b_hat", itemId = "item_hat")),
        )
        val draftState = CreateTripDraftState(
            initialSelectedSourceListIds = listOf(listA.id, listB.id),
            initialSelectedListEntryIds = setOf("entry_a_shirt", "entry_a_socks", "entry_b_hat"),
        )

        draftState.removeSourceItems("item_shirt", listOf(listA, listB))

        assertEquals(setOf("entry_a_socks", "entry_b_hat"), draftState.selectedListEntryIds)
        assertEquals(listOf("list_a", "list_b"), draftState.selectedSourceListIds)

        draftState.removeSourceItems("item_hat", listOf(listA, listB))

        assertEquals(setOf("entry_a_socks"), draftState.selectedListEntryIds)
        assertEquals(listOf("list_a"), draftState.selectedSourceListIds)
    }

    private fun packlyList(id: String, entries: List<PacklyListEntry>): PacklyList = PacklyList(
        id = id,
        name = id,
        entries = entries,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun packlyListEntry(id: String, itemId: String): PacklyListEntry = PacklyListEntry(
        id = id,
        itemId = itemId,
        itemNameSnapshot = itemId,
        categoryIdSnapshot = "category",
        sortOrder = 0,
    )
}
