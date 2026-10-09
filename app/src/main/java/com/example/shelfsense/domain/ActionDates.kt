package com.example.shelfsense.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DateDriver { PRINTED_DATE, OPENING_DEADLINE, SAME_DAY }

enum class Urgency { OVERDUE, TODAY, URGENT, SOON, LATER }

data class ActionInfo(
    val actionDate: LocalDate,
    val daysLeft: Long,
    val driver: DateDriver,
    val openingDeadline: LocalDate?,
    val urgency: Urgency
)

// the core rule from the proposal: an opened item is due on whichever comes first,
// the printed date or the opening date plus the package's use within period.
// nothing is inferred from the category, a missing instruction just means the printed date applies
object ActionDates {

    const val URGENT_DAYS = 2L
    const val SOON_DAYS = 5L

    fun openingDeadline(openedDate: LocalDate?, useWithinDays: Int?): LocalDate? =
        if (openedDate != null && useWithinDays != null && useWithinDays > 0) {
            openedDate.plusDays(useWithinDays.toLong())
        } else {
            null
        }

    fun actionDate(printedDate: LocalDate, openedDate: LocalDate?, useWithinDays: Int?): LocalDate {
        val deadline = openingDeadline(openedDate, useWithinDays) ?: return printedDate
        return if (deadline.isBefore(printedDate)) deadline else printedDate
    }

    fun evaluate(
        printedDate: LocalDate,
        openedDate: LocalDate?,
        useWithinDays: Int?,
        today: LocalDate = LocalDate.now()
    ): ActionInfo {
        val deadline = openingDeadline(openedDate, useWithinDays)
        val action = actionDate(printedDate, openedDate, useWithinDays)
        val driver = when {
            deadline == null -> DateDriver.PRINTED_DATE
            deadline.isBefore(printedDate) -> DateDriver.OPENING_DEADLINE
            deadline.isEqual(printedDate) -> DateDriver.SAME_DAY
            else -> DateDriver.PRINTED_DATE
        }
        val daysLeft = ChronoUnit.DAYS.between(today, action)
        return ActionInfo(action, daysLeft, driver, deadline, urgencyFor(daysLeft))
    }

    fun urgencyFor(daysLeft: Long): Urgency = when {
        daysLeft < 0 -> Urgency.OVERDUE
        daysLeft == 0L -> Urgency.TODAY
        daysLeft <= URGENT_DAYS -> Urgency.URGENT
        daysLeft <= SOON_DAYS -> Urgency.SOON
        else -> Urgency.LATER
    }

    fun daysLeftLabel(daysLeft: Long): String = when {
        daysLeft < -1 -> "${-daysLeft} days overdue"
        daysLeft == -1L -> "1 day overdue"
        daysLeft == 0L -> "Use today"
        daysLeft == 1L -> "1 day left"
        else -> "$daysLeft days left"
    }
}
