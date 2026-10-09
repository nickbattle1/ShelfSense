package com.example.shelfsense.data.model

// stored by name in Room and Firestore, so the constant names must never be renamed

enum class FoodCategory(val label: String) {
    FRUIT_VEG("Fruit and vegetables"),
    DAIRY("Dairy"),
    MEAT_SEAFOOD("Meat and seafood"),
    BAKERY("Bakery"),
    PANTRY_SAUCES("Pantry sauces"),
    DRY_GOODS("Dry goods"),
    FROZEN("Frozen"),
    LEFTOVERS("Leftovers");

    // a starting suggestion for the form, the user can always change it
    val usualStorage: StorageLocation
        get() = when (this) {
            FROZEN -> StorageLocation.FREEZER
            BAKERY, PANTRY_SAUCES, DRY_GOODS -> StorageLocation.PANTRY
            else -> StorageLocation.FRIDGE
        }
}

enum class StorageLocation(val label: String) {
    FRIDGE("Fridge"),
    FREEZER("Freezer"),
    PANTRY("Pantry")
}

enum class DateType(val label: String) {
    USE_BY("Use by"),
    BEST_BEFORE("Best before"),
    OWN_DATE("No date on the pack"),
    NO_EXPIRY("Doesn't expire");

    // honey, salt and spirits keep indefinitely, so they're the only items saved without a date
    val needsDate: Boolean get() = this != NO_EXPIRY

    // what the date field is called in the form for each type
    val dateLabel: String
        get() = if (this == OWN_DATE) "Use it by" else "Printed date"
}

enum class Outcome(val label: String, val pastTense: String) {
    CONSUMED("Consumed", "consumed"),
    DONATED("Donated", "donated"),
    DISCARDED("Discarded", "discarded")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

// option lists shared by the forms, carried over from the prototype
object Choices {
    val households = listOf("Just me", "Two people", "Three to four", "Five or more")
    val leadDays = listOf(1, 2, 3, 5, 7, 14, 30)
    const val USE_WITHIN_MAX = 365

    fun leadLabel(days: Int): String = when (days) {
        1 -> "1 day before"
        7 -> "1 week before"
        14 -> "2 weeks before"
        30 -> "1 month before"
        else -> "$days days before"
    }
}
