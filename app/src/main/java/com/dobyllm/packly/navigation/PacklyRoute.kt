package com.dobyllm.packly.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PacklyRoute {
    private const val TripIdArg = "tripId"
    private const val ListIdArg = "listId"

    const val Home = "home"
    const val Items = "items"
    const val Lists = "lists"
    const val Trips = "trips"
    const val Options = "options"
    const val CreateTripGraph = "createTrip"
    const val CreateTripDetails = "createTrip/details"
    const val CreateTripDeadline = "createTrip/deadline"
    const val CreateTripLists = "createTrip/lists"
    const val CreateTripItems = "createTrip/items"
    const val ListDetail = "lists/{$ListIdArg}"
    const val TripDetail = "trips/{$TripIdArg}"
    const val PackingMode = "trips/{$TripIdArg}/packing"

    fun listDetail(listId: String): String? = routeWithId("lists", listId)
    fun tripDetail(tripId: String): String? = routeWithId("trips", tripId)
    fun packingMode(tripId: String): String? = routeWithId("trips", tripId)?.let { "$it/packing" }

    private fun routeWithId(prefix: String, id: String): String? {
        val normalizedId = id.trim()
        return normalizedId.takeIf { it.isNotEmpty() }?.let { "$prefix/${it.encodePathSegment()}" }
    }

    private fun String.encodePathSegment(): String = URLEncoder
        .encode(this, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}
