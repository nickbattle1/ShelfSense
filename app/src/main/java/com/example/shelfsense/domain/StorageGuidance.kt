package com.example.shelfsense.domain

import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.StorageLocation

// general advice only. it never changes a date, and the package always wins
object StorageGuidance {

    const val PACKAGE_FIRST = "Instructions printed on the package always take precedence over general guidance."

    fun forLocation(location: StorageLocation): String = when (location) {
        StorageLocation.FRIDGE -> "Keep your fridge at 5°C or below, and don't overfill it so cold air can move around."
        StorageLocation.FREEZER -> "Keep it frozen solid, and thaw it in the fridge rather than on the bench."
        StorageLocation.PANTRY -> "Store it somewhere cool and dry, away from direct sunlight and the oven."
    }

    fun forCategory(category: FoodCategory): String = when (category) {
        FoodCategory.FRUIT_VEG -> "Most vegetables last longer in the crisper drawer. Potatoes and onions keep best somewhere cool and dark, out of the fridge."
        FoodCategory.DAIRY -> "Put the lid back on and return it to the fridge straight after use."
        FoodCategory.MEAT_SEAFOOD -> "Keep raw meat and seafood sealed on the bottom shelf so juices can't drip onto other food."
        FoodCategory.BAKERY -> "Bread goes stale faster in the fridge. Freeze slices you won't eat in the next few days."
        FoodCategory.PANTRY_SAUCES -> "Many sauces need refrigerating once opened, so check the label."
        FoodCategory.DRY_GOODS -> "Once opened, tip it into an airtight container to keep it fresh and keep pests out."
        FoodCategory.FROZEN -> "Label it with the date it went in so older packs get used first."
        FoodCategory.LEFTOVERS -> "Refrigerate leftovers once they stop steaming, and reheat them until piping hot."
    }

    // shown on Insights beside the category that gets thrown out most
    fun wasteTip(category: FoodCategory): String = when (category) {
        FoodCategory.FRUIT_VEG -> "Buy loose produce in smaller amounts, and plan a meal or two around what's already in the crisper."
        FoodCategory.DAIRY -> "Check what's open before you shop, and choose smaller tubs if they often go unfinished."
        FoodCategory.MEAT_SEAFOOD -> "Freeze meat you won't cook within a day or two of buying it."
        FoodCategory.BAKERY -> "Freeze half the loaf when you get home and toast slices straight from frozen."
        FoodCategory.PANTRY_SAUCES -> "Record the day you open a jar, so ShelfSense can work out the tighter date for you."
        FoodCategory.DRY_GOODS -> "Keep older packets at the front of the cupboard so they get used first."
        FoodCategory.FROZEN -> "Label frozen food with the date it went in, and eat the oldest first."
        FoodCategory.LEFTOVERS -> "Cook smaller batches, or freeze portions on the day you make them."
    }
}
