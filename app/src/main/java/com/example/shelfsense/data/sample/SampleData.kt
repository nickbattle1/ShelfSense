package com.example.shelfsense.data.sample

import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.model.StorageLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

// sample pantry for demos and testing, only offered in debug builds. it goes through the
// normal repository path, so it lands in Room and syncs to Firestore like anything else
object SampleData {

    // fixed ids, so loading twice rewrites the same rows instead of doubling them
    fun build(today: LocalDate = LocalDate.now()): List<PantryItem> =
        activeItems(today).mapIndexed { i, item -> item.copy(id = "sample-item-$i") } +
            history(today).mapIndexed { i, item -> item.copy(id = "sample-history-$i") }

    private fun activeItems(today: LocalDate): List<PantryItem> = listOf(
        PantryItem(
            name = "Tomatoes", category = FoodCategory.FRUIT_VEG, storage = StorageLocation.FRIDGE,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(2)
        ),
        PantryItem(
            name = "Milk", category = FoodCategory.DAIRY, storage = StorageLocation.FRIDGE,
            dateType = DateType.USE_BY, printedDate = today.plusDays(9),
            openedDate = today.minusDays(1), useWithinDays = 5
        ),
        PantryItem(
            name = "Lettuce", category = FoodCategory.FRUIT_VEG, storage = StorageLocation.FRIDGE,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(6)
        ),
        PantryItem(
            name = "Greek Yoghurt 500g", brand = "Chobani", barcode = "9300675024235",
            category = FoodCategory.DAIRY, storage = StorageLocation.FRIDGE,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(12),
            openedDate = today.minusDays(2), useWithinDays = 5
        ),
        PantryItem(
            name = "Baby spinach", category = FoodCategory.FRUIT_VEG, storage = StorageLocation.FRIDGE,
            dateType = DateType.USE_BY, printedDate = today.minusDays(1)
        ),
        PantryItem(
            name = "Chicken thighs", category = FoodCategory.MEAT_SEAFOOD, storage = StorageLocation.FRIDGE,
            dateType = DateType.USE_BY, printedDate = today.plusDays(1)
        ),
        PantryItem(
            name = "Pasta sauce", category = FoodCategory.PANTRY_SAUCES, storage = StorageLocation.FRIDGE,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(120),
            openedDate = today.minusDays(3), useWithinDays = 5
        ),
        PantryItem(
            name = "Sourdough", category = FoodCategory.BAKERY, storage = StorageLocation.PANTRY,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(4)
        ),
        PantryItem(
            name = "Basmati rice", category = FoodCategory.DRY_GOODS, storage = StorageLocation.PANTRY,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(180)
        ),
        PantryItem(
            name = "Frozen peas", category = FoodCategory.FROZEN, storage = StorageLocation.FREEZER,
            dateType = DateType.BEST_BEFORE, printedDate = today.plusDays(240)
        ),
        PantryItem(
            name = "Leftover curry", category = FoodCategory.LEFTOVERS, storage = StorageLocation.FRIDGE,
            dateType = DateType.USE_BY, printedDate = today.plusDays(2)
        ),
        PantryItem(
            name = "Apples", category = FoodCategory.FRUIT_VEG, storage = StorageLocation.FRIDGE,
            dateType = DateType.OWN_DATE, printedDate = today.plusDays(8)
        ),
        PantryItem(
            name = "Honey", category = FoodCategory.PANTRY_SAUCES, storage = StorageLocation.PANTRY,
            dateType = DateType.NO_EXPIRY, printedDate = null
        )
    )

    // six months of outcomes with fewer items binned each month, so the trend chart has a story to tell
    private fun history(today: LocalDate): List<PantryItem> {
        val random = Random(46)
        val zone = ZoneId.systemDefault()
        val names = mapOf(
            FoodCategory.FRUIT_VEG to listOf("Bananas", "Carrots", "Broccoli", "Strawberries", "Avocados", "Zucchini"),
            FoodCategory.DAIRY to listOf("Milk", "Cheddar", "Natural yoghurt", "Butter", "Cream"),
            FoodCategory.MEAT_SEAFOOD to listOf("Beef mince", "Salmon fillets", "Chicken breast", "Sausages"),
            FoodCategory.BAKERY to listOf("Sourdough", "Wraps", "Bread rolls", "Crumpets"),
            FoodCategory.PANTRY_SAUCES to listOf("Pesto", "Pasta sauce", "Hummus"),
            FoodCategory.DRY_GOODS to listOf("Rice crackers", "Penne", "Rolled oats"),
            FoodCategory.LEFTOVERS to listOf("Leftover pasta", "Leftover stir fry")
        )
        val categories = names.keys.toList()
        // consumed, donated and discarded for each of the last six months, oldest first
        val plan = listOf(
            Triple(4, 0, 4), Triple(5, 1, 3), Triple(6, 0, 3),
            Triple(6, 1, 2), Triple(7, 1, 2), Triple(5, 1, 1)
        )
        val items = mutableListOf<PantryItem>()
        plan.forEachIndexed { index, (consumed, donated, discarded) ->
            val month = YearMonth.from(today).minusMonths((plan.size - 1 - index).toLong())
            val lastDay = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
            val outcomes = List(consumed) { Outcome.CONSUMED } +
                List(donated) { Outcome.DONATED } +
                List(discarded) { Outcome.DISCARDED }
            outcomes.forEach { outcome ->
                // produce and dairy make up most of what gets thrown out, as they do in most kitchens
                val category = if (outcome == Outcome.DISCARDED && random.nextInt(3) != 0) {
                    if (random.nextBoolean()) FoodCategory.FRUIT_VEG else FoodCategory.DAIRY
                } else {
                    categories[random.nextInt(categories.size)]
                }
                val day = month.atDay(1 + random.nextInt(lastDay))
                val resolvedAt = day.atTime(LocalTime.of(8 + random.nextInt(12), random.nextInt(60)))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                    .coerceAtMost(System.currentTimeMillis())
                items += PantryItem(
                    name = names.getValue(category).random(random),
                    category = category,
                    storage = category.usualStorage,
                    dateType = if (category == FoodCategory.MEAT_SEAFOOD || category == FoodCategory.LEFTOVERS) {
                        DateType.USE_BY
                    } else {
                        DateType.BEST_BEFORE
                    },
                    printedDate = day.plusDays((random.nextInt(9) - 3).toLong()),
                    outcome = outcome,
                    resolvedAt = resolvedAt,
                    addedAt = resolvedAt - (2 + random.nextInt(8)) * DAY_MS
                )
            }
        }
        return items
    }

    private const val DAY_MS = 86_400_000L
}
