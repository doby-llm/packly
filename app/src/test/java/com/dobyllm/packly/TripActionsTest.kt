package com.dobyllm.packly

import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
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
    fun buildTripEntriesAppliesListDerivedQuantityOverridesWithoutDuplicatingSelectedItems() {
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
            itemIds = setOf("item_shirt", "item_socks"),
            itemQuantities = mapOf("item_shirt" to 4, "item_socks" to 2),
        )

        assertEquals(listOf("item_shirt", "item_socks"), entries.map { it.sourceItemId })
        assertEquals(listOf(4, 2), entries.map { it.quantity })
    }

    @Test
    fun buildTripEntriesHonorsSelectedSourceListEntriesAndKeepsQuantityOverrides() {
        val document = PacklyAppDocument(
            items = listOf(
                item(id = "item_shirt", name = "Shirt", categoryId = "cat_clothes"),
                item(id = "item_socks", name = "Socks", categoryId = "cat_clothes"),
                item(id = "item_hat", name = "Hat", categoryId = "cat_clothes"),
            ),
            lists = listOf(
                packingList(
                    id = "list_weekend",
                    entries = listOf(
                        listEntry(id = "list_entry_shirt", itemId = "item_shirt", name = "Shirt", categoryId = "cat_clothes", sortOrder = 0),
                        listEntry(id = "list_entry_socks", itemId = "item_socks", name = "Socks", categoryId = "cat_clothes", sortOrder = 1),
                    ),
                ),
            ),
        )

        val entries = document.buildTripEntries(
            sourceListIds = listOf("list_weekend"),
            itemIds = setOf("item_hat"),
            sourceListEntryIds = setOf("list_entry_socks"),
            itemQuantities = mapOf("item_socks" to 3, "item_hat" to 2),
        )

        assertEquals(listOf("item_socks", "item_hat"), entries.map { it.sourceItemId })
        assertEquals(listOf("list_entry_socks", null), entries.map { it.sourceListEntryId })
        assertEquals(listOf(3, 2), entries.map { it.quantity })
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
    fun addTripEntriesGroupsAddedEntriesByCategoryAndPreservesEntryState() {
        val packedAt = "2026-06-04T08:30:00Z"
        val clothes = tripEntry(
            id = "entry_shirt",
            sourceItemId = "item_shirt",
            name = "Shirt",
            categoryId = "cat_clothes",
            sortOrder = 0,
        )
        val documents = tripEntry(
            id = "entry_passport",
            sourceItemId = "item_passport",
            sourceListEntryId = "list_entry_passport",
            name = "Passport",
            categoryId = "cat_documents",
            notes = "keep safe",
            quantity = 2,
            isPacked = true,
            packedAt = packedAt,
            sortOrder = 1,
        )
        val trip = trip(entries = listOf(clothes, documents))

        val updated = trip.withAdditionalEntriesForTripAction(
            newEntryCandidates = listOf(
                tripEntry(
                    id = "entry_socks",
                    sourceItemId = "item_socks",
                    name = "Socks",
                    categoryId = "cat_clothes",
                    sortOrder = 2,
                ),
            ),
            now = "2026-06-05T09:00:00Z",
            categories = listOf(
                category(id = "cat_clothes", label = "Clothes", sortOrder = 0),
                category(id = "cat_documents", label = "Documents", sortOrder = 1),
            ),
        )

        assertEquals(listOf("entry_shirt", "entry_socks", "entry_passport"), updated.entries.map { it.id })
        assertEquals(listOf(0, 1, 2), updated.entries.map { it.sortOrder })
        val movedDocuments = updated.entries.single { it.id == "entry_passport" }
        assertEquals("item_passport", movedDocuments.sourceItemId)
        assertEquals("list_entry_passport", movedDocuments.sourceListEntryId)
        assertEquals("keep safe", movedDocuments.notes)
        assertEquals(2, movedDocuments.quantity)
        assertEquals(true, movedDocuments.isPacked)
        assertEquals(packedAt, movedDocuments.packedAt)
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
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

    @Test
    fun toggleSourceItemAddsWhenMissingAndRemovesMatchingEntryWhenPresent() {
        val item = item(id = "item_socks", name = "Socks", categoryId = "cat_clothes")
        val candidate = tripEntry(
            id = "entry_socks",
            sourceItemId = "item_socks",
            name = "Socks",
            categoryId = "cat_clothes",
            sortOrder = 0,
        )
        val emptyTrip = trip(entries = emptyList())

        val added = emptyTrip.withToggledSourceItemForTripAction(
            sourceItem = item,
            newEntryCandidates = listOf(candidate),
            now = "2026-06-05T09:00:00Z",
        )
        val removed = added.withToggledSourceItemForTripAction(
            sourceItem = item,
            newEntryCandidates = listOf(candidate.copy(id = "entry_duplicate")),
            now = "2026-06-05T10:00:00Z",
        )

        assertEquals(listOf("entry_socks"), added.entries.map { it.id })
        assertEquals("2026-06-05T09:00:00Z", added.updatedAt)
        assertEquals(emptyList<TripEntry>(), removed.entries)
        assertEquals("2026-06-05T10:00:00Z", removed.updatedAt)
    }

    @Test
    fun toggleSourceListRemovesAllListMembershipEntriesAndReindexesRemainingPlan() {
        val sourceList = packingList(
            id = "list_weekend",
            entries = listOf(
                listEntry(
                    id = "list_entry_socks",
                    itemId = "item_socks",
                    name = "Socks",
                    categoryId = "cat_clothes",
                    sortOrder = 0,
                ),
                listEntry(
                    id = "list_entry_hat",
                    itemId = null,
                    name = "Sun Hat",
                    categoryId = "cat_clothes",
                    sortOrder = 1,
                ),
            ),
        )
        val keep = tripEntry(id = "entry_keep", sourceItemId = "item_passport", name = "Passport", categoryId = "cat_docs", sortOrder = 9)
        val trip = trip(
            entries = listOf(
                keep,
                tripEntry(id = "entry_socks", sourceItemId = "item_socks", sourceListEntryId = "list_entry_socks", name = "Socks", categoryId = "cat_clothes", sortOrder = 2),
                tripEntry(id = "entry_hat", sourceItemId = null, sourceListEntryId = "list_entry_hat", name = " sun   hat ", categoryId = "cat_clothes", sortOrder = 5),
            ),
        )

        val updated = trip.withToggledSourceListForTripAction(
            sourceList = sourceList,
            newEntryCandidates = emptyList(),
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_keep"), updated.entries.map { it.id })
        assertEquals(0, updated.entries.single().sortOrder)
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
    }

    @Test
    fun toggleSourceListAddsOnlyMissingEntriesWhenPartiallyPresent() {
        val sourceList = packingList(
            id = "list_weekend",
            entries = listOf(
                listEntry(id = "list_entry_socks", itemId = "item_socks", name = "Socks", categoryId = "cat_clothes"),
                listEntry(id = "list_entry_hat", itemId = "item_hat", name = "Hat", categoryId = "cat_clothes"),
            ),
        )
        val existing = tripEntry(id = "entry_socks", sourceItemId = "item_socks", name = "Socks", categoryId = "cat_clothes", sortOrder = 0)
        val trip = trip(entries = listOf(existing))

        val updated = trip.withToggledSourceListForTripAction(
            sourceList = sourceList,
            newEntryCandidates = listOf(
                tripEntry(id = "entry_duplicate_socks", sourceItemId = "item_socks", name = "Socks", categoryId = "cat_clothes", sortOrder = 1),
                tripEntry(id = "entry_hat", sourceItemId = "item_hat", sourceListEntryId = "list_entry_hat", name = "Hat", categoryId = "cat_clothes", sortOrder = 2),
            ),
            now = "2026-06-05T09:00:00Z",
        )

        assertEquals(listOf("entry_socks", "entry_hat"), updated.entries.map { it.id })
        assertEquals(listOf(0, 1), updated.entries.map { it.sortOrder })
        assertEquals("2026-06-05T09:00:00Z", updated.updatedAt)
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

    private fun category(
        id: String,
        label: String,
        sortOrder: Int,
    ) = PacklyCategory(
        id = id,
        key = id.removePrefix("cat_"),
        label = label,
        iconKey = "category",
        accentColorHex = "#000000",
        softColorHex = "#ffffff",
        sortOrder = sortOrder,
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
