package com.dobyllm.packly.data.migration

import kotlinx.serialization.json.JsonObject

interface PacklyJsonMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(rawJson: JsonObject): JsonObject
}

object PacklyMigrationRunner {
    fun migrate(rawJson: JsonObject): JsonObject = rawJson
}
