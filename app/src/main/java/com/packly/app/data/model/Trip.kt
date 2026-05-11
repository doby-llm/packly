package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String,
    val name: String,
    val date: String,
    val listId: String? = null,
    val items: List&lt;TripEntry&gt; = emptyList(),
    val notes: String = "",
    val createdAt: Long
)
