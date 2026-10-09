package com.example.shelfsense.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ActionDatesTest {

    private val today = LocalDate.of(2026, 9, 10)

    private fun evaluate(printed: LocalDate?, opened: LocalDate?, within: Int?): ActionInfo =
        checkNotNull(ActionDates.evaluate(printed, opened, within, today))

    @Test
    fun unopenedItemUsesPrintedDate() {
        val info = evaluate(today.plusDays(8), null, null)
        assertEquals(today.plusDays(8), info.actionDate)
        assertEquals(DateDriver.PRINTED_DATE, info.driver)
        assertNull(info.openingDeadline)
        assertEquals(8L, info.daysLeft)
    }

    @Test
    fun openingDeadlineWinsWhenItComesFirst() {
        // yoghurt opened on the 10th, use within 5 days, best before the 18th
        val info = evaluate(LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 10), 5)
        assertEquals(LocalDate.of(2026, 9, 15), info.actionDate)
        assertEquals(DateDriver.OPENING_DEADLINE, info.driver)
        assertEquals(5L, info.daysLeft)
    }

    @Test
    fun printedDateWinsWhenItComesFirst() {
        val info = evaluate(today.plusDays(3), today, 30)
        assertEquals(today.plusDays(3), info.actionDate)
        assertEquals(DateDriver.PRINTED_DATE, info.driver)
        assertNotNull(info.openingDeadline)
    }

    @Test
    fun sameDayIsReportedSeparately() {
        val info = evaluate(today.plusDays(4), today, 4)
        assertEquals(DateDriver.SAME_DAY, info.driver)
        assertEquals(today.plusDays(4), info.actionDate)
    }

    @Test
    fun openedWithoutInstructionFallsBackToPrintedDate() {
        val info = evaluate(today.plusDays(6), today.minusDays(1), null)
        assertEquals(today.plusDays(6), info.actionDate)
        assertEquals(DateDriver.PRINTED_DATE, info.driver)
        assertNull(info.openingDeadline)
    }

    @Test
    fun zeroDayInstructionIsIgnored() {
        assertNull(ActionDates.openingDeadline(today, 0))
        assertEquals(today.plusDays(9), ActionDates.actionDate(today.plusDays(9), today, 0))
    }

    @Test
    fun itemThatDoesNotExpireHasNoActionDate() {
        assertNull(ActionDates.evaluate(null, null, null, today))
        assertNull(ActionDates.evaluate(null, today, null, today))
    }

    @Test
    fun undatedItemUsesItsOpeningDeadline() {
        // an opened jar with nothing printed but "use within 14 days" on the label
        val info = evaluate(null, today, 14)
        assertEquals(today.plusDays(14), info.actionDate)
        assertEquals(DateDriver.OPENING_DEADLINE, info.driver)
    }

    @Test
    fun urgencyBandsFollowDaysLeft() {
        assertEquals(Urgency.OVERDUE, ActionDates.urgencyFor(-1))
        assertEquals(Urgency.TODAY, ActionDates.urgencyFor(0))
        assertEquals(Urgency.URGENT, ActionDates.urgencyFor(1))
        assertEquals(Urgency.URGENT, ActionDates.urgencyFor(2))
        assertEquals(Urgency.SOON, ActionDates.urgencyFor(3))
        assertEquals(Urgency.SOON, ActionDates.urgencyFor(5))
        assertEquals(Urgency.LATER, ActionDates.urgencyFor(6))
    }

    @Test
    fun daysLeftLabelsReadNaturally() {
        assertEquals("3 days overdue", ActionDates.daysLeftLabel(-3))
        assertEquals("1 day overdue", ActionDates.daysLeftLabel(-1))
        assertEquals("Use today", ActionDates.daysLeftLabel(0))
        assertEquals("1 day left", ActionDates.daysLeftLabel(1))
        assertEquals("4 days left", ActionDates.daysLeftLabel(4))
    }
}
