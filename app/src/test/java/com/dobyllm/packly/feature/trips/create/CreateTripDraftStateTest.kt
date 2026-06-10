package com.dobyllm.packly.feature.trips.create

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
}
