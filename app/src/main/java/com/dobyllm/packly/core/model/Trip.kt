package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TripStatus { Draft, Active, Completed, Archived }

@Serializable
data class PacklyTrip(
    val id: TripId,
    val name: String,
    val destination: String = "",
    val startDate: LocalDateString? = null,
    val endDate: LocalDateString? = null,
    val sourceListId: ListId? = null,
    val packBy: InstantString? = null,
    val status: TripStatus = TripStatus.Active,
    val entries: List<TripEntry> = emptyList(),
    val createdAt: InstantString,
    val updatedAt: InstantString,
    val completedAt: InstantString? = null,
)

@Serializable
data class TripEntry(
    val id: TripEntryId,
    val sourceItemId: ItemId?,
    val sourceListEntryId: ListEntryId?,
    val nameSnapshot: String,
    val categoryIdSnapshot: CategoryId,
    val quantity: Int = 1,
    val notes: String = "",
    val isPacked: Boolean = false,
    val packedAt: InstantString? = null,
    val sortOrder: Int,
)
