package com.dobyllm.packly.feature.items

import com.dobyllm.packly.core.model.PacklyItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemsFilterTest {
    @Test
    fun filters_by_query_category_and_active_status_by_default() {
        val result = filterLibraryItems(
            items = sampleItems,
            query = "tooth",
            selectedCategoryIds = setOf("cat_toiletries"),
            statusFilter = ItemStatusFilter.Active,
        )

        assertEquals(listOf("item_toothbrush"), result.map { it.id })
    }

    @Test
    fun archived_status_filter_returns_archived_items_only() {
        val result = filterLibraryItems(
            items = sampleItems,
            query = "",
            selectedCategoryIds = emptySet(),
            statusFilter = ItemStatusFilter.Archived,
        )

        assertEquals(listOf("item_old_sunscreen"), result.map { it.id })
    }

    @Test
    fun all_status_filter_keeps_category_grouping_input_order() {
        val result = filterLibraryItems(
            items = sampleItems,
            query = "",
            selectedCategoryIds = setOf("cat_toiletries"),
            statusFilter = ItemStatusFilter.All,
        )

        assertEquals(listOf("item_toothbrush", "item_old_sunscreen"), result.map { it.id })
    }

    private companion object {
        private val sampleItems = listOf(
            item(id = "item_toothbrush", name = "Toothbrush", categoryId = "cat_toiletries"),
            item(id = "item_passport", name = "Passport", categoryId = "cat_documents"),
            item(id = "item_old_sunscreen", name = "Old sunscreen", categoryId = "cat_toiletries", isArchived = true),
        )

        private fun item(
            id: String,
            name: String,
            categoryId: String,
            isArchived: Boolean = false,
        ) = PacklyItem(
            id = id,
            name = name,
            categoryId = categoryId,
            notes = "",
            isArchived = isArchived,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
    }
}
