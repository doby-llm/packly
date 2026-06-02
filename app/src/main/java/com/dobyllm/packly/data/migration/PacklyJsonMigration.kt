package com.dobyllm.packly.data.migration

import com.dobyllm.packly.core.model.CurrentSchemaVersion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

interface PacklyJsonMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(rawJson: JsonObject): JsonObject
}

object PacklyMigrationRunner {
    private val migrations = listOf(V1ToV2QuantityMigration)

    fun migrate(rawJson: JsonObject): JsonObject {
        var current = rawJson
        var version = current.schemaVersion()

        while (version < CurrentSchemaVersion) {
            val migration = migrations.firstOrNull { it.fromVersion == version }
                ?: return current.withSchemaVersion(CurrentSchemaVersion)
            current = migration.migrate(current)
            version = current.schemaVersion()
        }

        return current
    }
}

private object V1ToV2QuantityMigration : PacklyJsonMigration {
    override val fromVersion: Int = 1
    override val toVersion: Int = 2

    override fun migrate(rawJson: JsonObject): JsonObject = rawJson.withSchemaVersion(toVersion)
}

private fun JsonObject.schemaVersion(): Int =
    this["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1

private fun JsonObject.withSchemaVersion(version: Int): JsonObject =
    JsonObject(this + ("schemaVersion" to JsonPrimitive(version)))
