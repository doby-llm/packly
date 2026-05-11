package com.packly.app.data.model

import kotlinx.serialization.Serializable

/** Mode determines how the list is populated during creation. */
enum class ListMode { CHECKLIST, TINDER }

@Serializable
data class ItemList(
    val id: String,
    val name: String,
    val mode: String = "checklist", // "checklist" | "tinder" — serialised as string for JSON compat
    val items: List(ListEntry) = emptyList(),
    val createdAt: Long
) {
    val listMode: ListMode get() = if (mode == "tinder") ListMode.TINDER else ListMode.CHECKLIST
}
