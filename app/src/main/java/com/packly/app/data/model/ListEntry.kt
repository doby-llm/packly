package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ListEntry(
    val itemId: String,
    val quantity: Int = 1,
    val checked: Boolean = false
)
