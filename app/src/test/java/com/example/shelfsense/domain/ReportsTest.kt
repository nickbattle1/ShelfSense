package com.example.shelfsense.domain

import com.example.shelfsense.data.local.OutcomeCount
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsTest {

    @Test
    fun avoidanceRateMatchesThePrototypeFigures() {
        // 19 used and 2 donated out of 25 resolved
        val totals = OutcomeTotals(consumed = 19, donated = 2, discarded = 4)
        assertEquals(25, totals.resolved)
        assertEquals(84, totals.avoidanceRate)
    }

    @Test
    fun noOutcomesMeansNoRate() {
        assertNull(OutcomeTotals().avoidanceRate)
    }

    @Test
    fun countsFromRoomAreGroupedByOutcome() {
        val totals = listOf(
            OutcomeCount(Outcome.CONSUMED, 5),
            OutcomeCount(Outcome.DISCARDED, 2)
        ).toTotals()
        assertEquals(OutcomeTotals(consumed = 5, donated = 0, discarded = 2), totals)
    }

    @Test
    fun useSoonFilterFollowsTheLeadTime() {
        assertTrue(PantryFilter.SOON.matches(2, false, StorageLocation.PANTRY, leadDays = 2))
        assertFalse(PantryFilter.SOON.matches(3, false, StorageLocation.PANTRY, leadDays = 2))
        assertTrue(PantryFilter.SOON.matches(-1, false, StorageLocation.FRIDGE, leadDays = 1))
    }

    @Test
    fun undatedItemsAreNeverDueSoon() {
        assertFalse(PantryFilter.SOON.matches(null, false, StorageLocation.PANTRY, leadDays = 30))
        assertTrue(PantryFilter.PANTRY.matches(null, false, StorageLocation.PANTRY, leadDays = 2))
    }

    @Test
    fun unknownFilterNamesFallBackToAll() {
        assertEquals(PantryFilter.SOON, PantryFilter.from("SOON"))
        assertEquals(PantryFilter.ALL, PantryFilter.from("nonsense"))
        assertEquals(PantryFilter.ALL, PantryFilter.from(null))
    }
}
