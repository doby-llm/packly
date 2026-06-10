package com.dobyllm.packly.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PacklyRouteTest {
    @Test
    fun tripRoutesRejectBlankIds() {
        assertNull(PacklyRoute.tripDetail(""))
        assertNull(PacklyRoute.tripDetail("   "))
        assertNull(PacklyRoute.packingMode(""))
        assertNull(PacklyRoute.packingMode("   "))
    }

    @Test
    fun tripRoutesTrimAndEncodePathSegments() {
        assertEquals("trips/trip%201%2F2", PacklyRoute.tripDetail(" trip 1/2 "))
        assertEquals("trips/trip%201%2F2/packing", PacklyRoute.packingMode(" trip 1/2 "))
    }

    @Test
    fun listRoutesRejectBlankIdsAndEncodePathSegments() {
        assertNull(PacklyRoute.listDetail(""))
        assertNull(PacklyRoute.listDetail("   "))
        assertEquals("lists/weekend%2Fcarry-on", PacklyRoute.listDetail("weekend/carry-on"))
    }
}
