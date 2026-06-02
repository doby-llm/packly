package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PacklyList(
    val id: ListId,
    val name: String,
    val description: String = "",
    val entries: List<PacklyListEntry> = emptyList(),
    val isSeed: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: InstantString,
    val updatedAt: InstantString,
)

@Serializable
data class PacklyListEntry(
    val id: ListEntryId,
    val itemId: ItemId?,
    val itemNameSnapshot: String,
    val categoryIdSnapshot: CategoryId,
    val quantity: Int = 1,
    val notes: String = "",
    val sortOrder: Int,
)
