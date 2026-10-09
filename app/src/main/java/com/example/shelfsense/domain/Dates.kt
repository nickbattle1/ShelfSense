package com.example.shelfsense.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// date formatting plus the conversions the Material date picker needs, since it works in UTC millis
object Dates {
    private val longFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
    private val shortFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun long(date: LocalDate): String = date.format(longFormat)
    fun dayMonth(date: LocalDate): String = date.format(dayMonthFormat)
    fun short(date: LocalDate): String = date.format(shortFormat)

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    // "Today, 7:02 am", "Yesterday, 9:14 pm" or "12 Sep, 7:02 am"
    fun relative(millis: Long, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        val moment = Instant.ofEpochMilli(millis).atZone(zone)
        val time = moment.format(timeFormat).lowercase(Locale.getDefault())
        val day = when (moment.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> short(moment.toLocalDate())
        }
        return "$day, $time"
    }
}
