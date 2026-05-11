package com.packly.app.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val EDIT_ITEMS = "edit_items"
    const val LISTS = "lists"
    const val CREATE_LIST = "create_list"
    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip_detail/{tripId}"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"
}
