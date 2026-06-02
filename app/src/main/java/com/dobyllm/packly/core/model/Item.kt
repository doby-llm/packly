package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PacklyItem(
    val id: ItemId,
    val name: String,
    val categoryId: CategoryId,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val isSeed: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: InstantString,
    val updatedAt: InstantString,
)
