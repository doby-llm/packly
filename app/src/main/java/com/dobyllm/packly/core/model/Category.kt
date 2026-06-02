package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PacklyCategory(
    val id: CategoryId,
    val key: String,
    val label: String,
    val iconKey: String,
    val accentColorHex: String,
    val softColorHex: String,
    val sortOrder: Int,
    val isSeed: Boolean = true,
    val isArchived: Boolean = false,
)
