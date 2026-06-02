package com.dobyllm.packly.domain.repository

import com.dobyllm.packly.core.model.*
import kotlinx.coroutines.flow.Flow

interface PacklyRepository {
    val appState: Flow<PacklyAppDocument>
    suspend fun updateDocument(transform: (PacklyAppDocument) -> PacklyAppDocument)
    suspend fun updateItems(transform: (List<PacklyItem>) -> List<PacklyItem>)
    suspend fun updateLists(transform: (List<PacklyList>) -> List<PacklyList>)
    suspend fun updateTrips(transform: (List<PacklyTrip>) -> List<PacklyTrip>)
    suspend fun updateSettings(transform: (PacklySettings) -> PacklySettings)
}
