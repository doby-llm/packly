package com.dobyllm.packly.data.json

import androidx.datastore.core.Serializer
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.data.seed.SeedDataProvider
import java.io.InputStream
import java.io.OutputStream

object PacklySerializer : Serializer<PacklyAppDocument> {
    override val defaultValue: PacklyAppDocument = SeedDataProvider.initialDocument()

    override suspend fun readFrom(input: InputStream): PacklyAppDocument =
        runCatching { PacklyJson.format.decodeFromString(PacklyAppDocument.serializer(), input.readBytes().decodeToString()) }
            .getOrElse { defaultValue }

    override suspend fun writeTo(t: PacklyAppDocument, output: OutputStream) {
        output.write(PacklyJson.format.encodeToString(PacklyAppDocument.serializer(), t).encodeToByteArray())
    }
}
