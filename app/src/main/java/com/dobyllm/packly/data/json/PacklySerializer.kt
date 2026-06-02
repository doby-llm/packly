package com.dobyllm.packly.data.json

import androidx.datastore.core.Serializer
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.data.migration.PacklyMigrationRunner
import com.dobyllm.packly.data.seed.SeedDataProvider
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

object PacklySerializer : Serializer<PacklyAppDocument> {
    override val defaultValue: PacklyAppDocument = SeedDataProvider.initialDocument()

    override suspend fun readFrom(input: InputStream): PacklyAppDocument = runCatching {
        val rawDocument = PacklyJson.format.parseToJsonElement(input.readBytes().decodeToString()).jsonObject
        val migratedDocument = PacklyMigrationRunner.migrate(rawDocument)
        PacklyJson.format.decodeFromJsonElement(PacklyAppDocument.serializer(), migratedDocument)
    }.getOrElse { defaultValue }

    override suspend fun writeTo(t: PacklyAppDocument, output: OutputStream) {
        output.write(PacklyJson.format.encodeToString(PacklyAppDocument.serializer(), t).encodeToByteArray())
    }
}
