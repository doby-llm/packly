package com.dobyllm.packly.data.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi

object PacklyJson {
    @OptIn(ExperimentalSerializationApi::class)
    val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }
}
