package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    val name: String,
    val categoryId: String,
    val iconName: String,
    val createdAt: Long
)
