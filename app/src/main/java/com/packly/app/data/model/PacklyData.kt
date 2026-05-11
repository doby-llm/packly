package com.packly.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PacklyData(
    val version: Int = 1,
    val categories: List&lt;Category&gt; = emptyList(),
    val items: List&lt;Item&gt; = emptyList(),
    val lists: List&lt;ItemList&gt; = emptyList(),
    val trips: List&lt;Trip&gt; = emptyList()
)
