package com.example.shelfsense.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

// dates are stored as epoch days, which keeps them sortable in SQL and free of time zones
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
