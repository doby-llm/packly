package com.packly.app.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val EDIT_ITEMS = "edit_items"
    const val LISTS = "lists"
    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val CREATE_LIST_CHECKLIST = "create_list_checklist/{listName}"
    const val CREATE_LIST_TINDER = "create_list_tinder/{listName}"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    fun createListChecklist(listName: String) = "create_list_checklist/$listName"
    fun createListTinder(listName: String) = "create_list_tinder/$listName"
}
