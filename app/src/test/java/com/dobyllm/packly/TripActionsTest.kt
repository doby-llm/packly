package com.dobyllm.packly

import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.model.TripEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TripActionsTest {
    @Test
    fun buildTripEntriesDefaultsQuantityToOneForListAndItemEntries() {
        val document = PacklyAppDocument(
            items = listOf(
                item(id = "item_shirt", name = "Shirt", categoryId = "cat_clothes"),
                item(id = "item_socks", name = "Socks", categoryId = "cat_clothes"),
            ),
            lists = listOf(
                packingList(
                    id = "list_weekend",
                    entries = listOf(
                        listEntry(
                            id = "list_entry_shirt",
                            itemId = "item_shirt",
                            name = "Shirt",
                            categoryId = "cat_clothes",
                        ),
                    ),
                ),
            ),
        )

        val entries = document.buildTripEntries(
            sourceListIds = listOf("list_weekend"),
            itemIds = setOf("item_socks"),
            itemQuantities = emptyMap(),
        )

        assertEquals(listOf("item_shirt", "item_socks"), entries.map { it.sourceItemId })
        assertEquals(listOf(1, 1), entries.map { it.quantity })
    }

    @Test
    fun addTripEntriesSkipsSourceItemDuplicatesBySourceItemIdAndPreservesExistingEntries() {
        val existing = tripEntry(
            id = "entry_existing",
            sourceItemId = "item_toothbrush",
            name = "Toothbrush",
            categoryId = "cat_toiletries",
            quantity = 3,
            isPacked = true,
            packedAt = "2026-06-04T08:30:00Z",
            sortOrder = 0,
        )
        val trip = trip(entries = listOf(existing))

        val updated = trip.withAdditionalEntriesForTripAction(
            newEntryCandidates = listOf(
                tripEntry(
                    id = "entry_duplicate",
                    sourceItemId = "item_toothbrush",
                    name = "Travel toothbrush",
                    categoryId = "cat_toiletries",
                    sortOrder = 1,
                ),
                tripEntry(
                    id = "entry_new",
                    sourceItemId = "item_passport",
                    name = "Passport",
                    categoryId = "cat_documents",
                    sortOrder = 2,
                ),
            ),
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_existing", "entry_new"), updated.entries.map { it.id })
        assertEquals(existing, updated.entries.first())
        assertEquals(1, updated.entries.last().sortOrder)
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
    }

    @Test
    fun addTripEntriesSkipsSnapshotOnlyDuplicatesByNormalizedNameAndCategory() {
        val trip = trip(
            entries = listOf(
                tripEntry(
                    id = "entry_existing",
                    sourceItemId = null,
                    name = "  Rain   Jacket ",
                    categoryId = "cat_clothes",
                    sortOrder = 0,
                ),
            ),
        )

        val updated = trip.withAdditionalEntriesForTripAction(
            newEntryCandidates = listOf(
                tripEntry(
                    id = "entry_duplicate",
                    sourceItemId = null,
                    name = "rain jacket",
                    categoryId = "cat_clothes",
                    sortOrder = 1,
                ),
                tripEntry(
                    id = "entry_new",
                    sourceItemId = null,
                    name = "Rain jacket",
                    categoryId = "cat_outdoors",
                    sortOrder = 2,
                ),
            ),
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_existing", "entry_new"), updated.entries.map { it.id })
        assertEquals(listOf(0, 1), updated.entries.map { it.sortOrder })
    }

    @Test
    fun addTripEntriesSkipsSourceItemWhenItCollidesWithExistingSnapshotOnlyNameAndCategory() {
        val trip = trip(
            entries = listOf(
                tripEntry(
                    id = "entry_snapshot_socks",
                    sourceItemId = null,
                    name = "Socks",
                    categoryId = "cat_clothes",
                    sortOrder = 0,
                ),
            ),
        )

        val updated = trip.withAdditionalEntriesForTripAction(
            newEntryCandidates = listOf(
                tripEntry(
                    id = "entry_source_socks",
                    sourceItemId = "item_socks",
                    name = " socks ",
                    categoryId = "cat_clothes",
                    sortOrder = 1,
                ),
            ),
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_snapshot_socks"), updated.entries.map { it.id })
        assertEquals("2026-06-01T09:00:00Z", updated.updatedAt)
    }

    @Test
    fun updateTripEntryQuantityCoercesToOneAndPreservesPackedState() {
        val packedAt = "2026-06-04T08:30:00Z"
        val original = tripEntry(
            id = "entry_socks",
            sourceItemId = "item_socks",
            name = "Socks",
            categoryId = "cat_clothes",
            quantity = 4,
            isPacked = true,
            packedAt = packedAt,
            sortOrder = 0,
        )

        val updated = trip(entries = listOf(original)).withUpdatedEntryQuantityForTripAction(
            entryId = "entry_socks",
            quantity = 0,
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(1, updated.entries.single().quantity)
        assertEquals(true, updated.entries.single().isPacked)
        assertEquals(packedAt, updated.entries.single().packedAt)
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
    }

    @Test
    fun removeTripEntryRemovesOneEntryReindexesAndPreservesRemainingState() {
        val packedAt = "2026-06-04T08:30:00Z"
        val trip = trip(
            entries = listOf(
                tripEntry(
                    id = "entry_late",
                    sourceItemId = "item_late",
                    sourceListEntryId = "list_entry_late",
                    name = "Late item",
                    categoryId = "cat_misc",
                    notes = "keep notes",
                    quantity = 5,
                    isPacked = true,
                    packedAt = packedAt,
                    sortOrder = 20,
                ),
                tripEntry(
                    id = "entry_remove",
                    sourceItemId = "item_remove",
                    name = "Remove me",
                    categoryId = "cat_misc",
                    sortOrder = 10,
                ),
                tripEntry(
                    id = "entry_early",
                    sourceItemId = null,
                    sourceListEntryId = "list_entry_early",
                    name = "Early item",
                    categoryId = "cat_misc",
                    notes = "snapshot notes",
                    quantity = 2,
                    sortOrder = 0,
                ),
            ),
        )

        val updated = trip.withRemovedEntryForTripAction(
            entryId = "entry_remove",
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_early", "entry_late"), updated.entries.map { it.id })
        assertEquals(listOf(0, 1), updated.entries.map { it.sortOrder })
        val late = updated.entries.single { it.id == "entry_late" }
        assertEquals(5, late.quantity)
        assertEquals(true, late.isPacked)
        assertEquals(packedAt, late.packedAt)
        assertEquals("item_late", late.sourceItemId)
        assertEquals("list_entry_late", late.sourceListEntryId)
        assertEquals("keep notes", late.notes)
        assertEquals("cat_misc", late.categoryIdSnapshot)
        assertEquals("Late item", late.nameSnapshot)
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
    }

    @Test
    fun removeTripEntryNoOpsWhenEntryIsMissing() {
        val original = trip(entries = listOf(tripEntry(id = "entry_keep", sortOrder = 0)))

        val updated = original.withRemovedEntryForTripAction(
            entryId = "entry_missing",
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(original, updated)
    }

    private fun item(
        id: String,
        name: String,
        categoryId: String,
    ) = PacklyItem(
        id = id,
        name = name,
        categoryId = categoryId,
        createdAt = "2026-06-01T09:00:00Z",
        updatedAt = "2026-06-01T09:00:00Z",
    )

    private fun packingList(
        id: String,
        entries: List<PacklyListEntry>,
    ) = PacklyList(
        id = id,
        name = "Weekend",
        entries = entries,
        createdAt = "2026-06-01T09:00:00Z",
        updatedAt = "2026-06-01T09:00:00Z",
    )

    private fun listEntry(
        id: String,
        itemId: String?,
        name: String,
        categoryId: String,
        sortOrder: Int = 0,
    ) = PacklyListEntry(
        id = id,
        itemId = itemId,
        itemNameSnapshot = name,
        categoryIdSnapshot = categoryId,
        sortOrder = sortOrder,
    )

    private fun trip(
        entries: List<TripEntry>,
    ) = PacklyTrip(
        id = "trip_weekend",
        name = "Weekend",
        destination = "Zurich",
        entries = entries,
        createdAt = "2026-06-01T09:00:00Z",
        updatedAt = "2026-06-01T09:00:00Z",
    )

    private fun tripEntry(
        id: String,
        sourceItemId: String? = null,
        sourceListEntryId: String? = null,
        name: String = "Item",
        categoryId: String = "cat_misc",
        notes: String = "",
        quantity: Int = 1,
        isPacked: Boolean = false,
        packedAt: String? = null,
        sortOrder: Int,
    ) = TripEntry(
        id = id,
        sourceItemId = sourceItemId,
        sourceListEntryId = sourceListEntryId,
        nameSnapshot = name,
        categoryIdSnapshot = categoryId,
        notes = notes,
        quantity = quantity,
        isPacked = isPacked,
        packedAt = packedAt,
        sortOrder = sortOrder,
    )
}
