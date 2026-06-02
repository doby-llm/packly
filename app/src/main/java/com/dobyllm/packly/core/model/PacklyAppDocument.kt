package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

const val CurrentSchemaVersion = 1

@Serializable
data class PacklyAppDocument(
    val schemaVersion: Int = CurrentSchemaVersion,
    val items: List<PacklyItem> = emptyList(),
    val lists: List<PacklyList> = emptyList(),
    val trips: List<PacklyTrip> = emptyList(),
    val categories: List<PacklyCategory> = emptyList(),
    val settings: PacklySettings = PacklySettings(),
    val session: PacklySessionState = PacklySessionState(),
)
