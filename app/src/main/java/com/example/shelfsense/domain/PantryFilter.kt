package com.example.shelfsense.domain

import com.example.shelfsense.data.model.StorageLocation

enum class PantryFilter(val label: String) {
    ALL("All"),
    SOON("Use soon"),
    OPENED("Opened"),
    FRIDGE("Fridge"),
    PANTRY("Pantry"),
    FREEZER("Freezer");

    // "use soon" follows the reminder lead time, so the list matches what the notification promised.
    // items with no date never count as due
    fun matches(daysLeft: Long?, opened: Boolean, storage: StorageLocation, leadDays: Int): Boolean = when (this) {
        ALL -> true
        SOON -> daysLeft != null && daysLeft <= leadDays
        OPENED -> opened
        FRIDGE -> storage == StorageLocation.FRIDGE
        PANTRY -> storage == StorageLocation.PANTRY
        FREEZER -> storage == StorageLocation.FREEZER
    }

    companion object {
        fun from(name: String?): PantryFilter = entries.firstOrNull { it.name == name } ?: ALL
    }
}
