package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val sortOrder: Int = 0
)
