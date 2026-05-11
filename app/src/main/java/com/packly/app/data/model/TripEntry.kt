package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TripEntry(
    val itemId: String,
    val quantity: Int = 1,
    val packed: Boolean = false
)
