package com.dobyllm.packly.core.time

import java.time.Instant
import java.util.UUID

object PacklyClock { fun now(): String = Instant.now().toString() }
object PacklyIds {
    fun item(): String = "item_${UUID.randomUUID()}"
    fun list(): String = "list_${UUID.randomUUID()}"
    fun listEntry(): String = "list_entry_${UUID.randomUUID()}"
    fun trip(): String = "trip_${UUID.randomUUID()}"
    fun tripEntry(): String = "trip_entry_${UUID.randomUUID()}"
}
