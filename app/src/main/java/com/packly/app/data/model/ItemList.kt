package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemList(
    val id: String,
    val name: String,
    val items: List&lt;ListEntry&gt; = emptyList(),
    val createdAt: Long
)
