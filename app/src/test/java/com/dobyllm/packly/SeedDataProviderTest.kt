package com.dobyllm.packly

import com.dobyllm.packly.data.seed.SeedDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataProviderTest {
    @Test
    fun seed_document_contains_default_categories_items_and_lists() {
        val doc = SeedDataProvider.initialDocument()

        assertEquals(10, doc.categories.size)
        assertTrue(doc.items.size >= 24)
        assertEquals(5, doc.lists.size)
        assertTrue(doc.lists.all { it.entries.isNotEmpty() })
    }

    @Test
    fun starter_lists_snapshot_item_names_and_categories() {
        val itemIds = SeedDataProvider.items.map { it.id }.toSet()

        SeedDataProvider.lists.flatMap { it.entries }.forEach { entry ->
            assertTrue(entry.itemId in itemIds)
            assertTrue(entry.itemNameSnapshot.isNotBlank())
            assertTrue(entry.categoryIdSnapshot.startsWith("cat_"))
        }
    }
}
