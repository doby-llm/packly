package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PacklyData(
    val version: Int = 1,
    val categories: List<Category> = emptyList(),
    val items: List<Item> = emptyList(),
    val lists: List<ItemList> = emptyList(),
    val trips: List<Trip> = emptyList()
)
