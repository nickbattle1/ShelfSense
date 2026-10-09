package com.example.shelfsense.domain

import com.example.shelfsense.data.model.DateType
import java.time.LocalDate

// the plain language copy behind the detail card and the "Why this date?" sheet
object DateExplainer {

    fun headline(info: ActionInfo, dateType: DateType): String = when (info.urgency) {
        Urgency.OVERDUE -> when (info.driver) {
            DateDriver.OPENING_DEADLINE -> "Opening deadline has passed"
            else -> "${dateType.phrase.replaceFirstChar { it.uppercase() }} date has passed"
        }
        Urgency.TODAY -> "Use it today"
        else -> if (info.daysLeft == 1L) "Use it by tomorrow" else "Use first in ${info.daysLeft} days"
    }

    // an extra line for overdue items, because use-by and best-before mean very different things
    fun overdueAdvice(info: ActionInfo, dateType: DateType, useWithinDays: Int?): String? {
        if (info.urgency != Urgency.OVERDUE) return null
        return when {
            info.driver == DateDriver.OPENING_DEADLINE ->
                "The package said to use it within ${plural(useWithinDays ?: 0, "day")} of opening. If in doubt, throw it out."
            dateType == DateType.USE_BY ->
                "Food past its use-by date can be unsafe even if it looks and smells fine, so don't eat it."
            else ->
                "It may still be safe if it has been stored properly. Check that it looks, smells and tastes right first."
        }
    }

    fun reasons(
        printedDate: LocalDate,
        dateType: DateType,
        openedDate: LocalDate?,
        useWithinDays: Int?,
        info: ActionInfo
    ): List<String> {
        val printed = "the printed ${dateType.phrase} date of ${Dates.long(printedDate)}"
        if (openedDate == null) {
            return listOf("This hasn't been opened yet, so ShelfSense goes by $printed.")
        }
        val deadline = info.openingDeadline
        if (useWithinDays == null || deadline == null) {
            return listOf(
                "You recorded this as opened on ${Dates.long(openedDate)}, but no after-opening instruction was entered.",
                "ShelfSense never guesses how long opened food lasts, so it goes by $printed.",
                "Check the package for a line such as \"use within 3 days of opening\" and add it for a tighter date."
            )
        }
        val comparison = when (info.driver) {
            DateDriver.OPENING_DEADLINE ->
                "That gives an opening deadline of ${Dates.long(deadline)}, which falls before $printed."
            DateDriver.SAME_DAY ->
                "That gives an opening deadline of ${Dates.long(deadline)}, the same day as $printed."
            DateDriver.PRINTED_DATE ->
                "That gives an opening deadline of ${Dates.long(deadline)}, but $printed comes first, so the printed date still applies."
        }
        return listOf(
            "You recorded this product as opened on ${Dates.long(openedDate)}.",
            "You entered the package instruction to use it within ${plural(useWithinDays, "day")} of opening.",
            comparison
        )
    }

    fun dateTypeNote(dateType: DateType): String = when (dateType) {
        DateType.USE_BY ->
            "A use-by date is about safety. Food shouldn't be eaten after it, even if it looks fine."
        DateType.BEST_BEFORE ->
            "A best-before date is about quality. Food is usually still safe after it if it's stored properly and undamaged, but it may not be at its best."
    }

    fun plural(count: Int, unit: String): String = if (count == 1) "1 $unit" else "$count ${unit}s"
}
