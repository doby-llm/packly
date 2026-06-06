package com.dobyllm.packly.navigation

object PacklyRoute {
    const val Home = "home"
    const val Items = "items"
    const val Lists = "lists"
    const val Trips = "trips"
    const val Options = "options"
    const val ListDetail = "lists/{listId}"
    const val TripDetail = "trips/{tripId}"
    const val PackingMode = "trips/{tripId}/packing"
    fun listDetail(listId: String) = "lists/$listId"
    fun tripDetail(tripId: String) = "trips/$tripId"
    fun packingMode(tripId: String) = "trips/$tripId/packing"
}
