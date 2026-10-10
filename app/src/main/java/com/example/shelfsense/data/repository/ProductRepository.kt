package com.example.shelfsense.data.repository

import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.remote.OpenFoodFactsApi
import com.example.shelfsense.data.remote.ProductDto
import com.example.shelfsense.data.remote.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException

data class ProductInfo(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val category: FoodCategory?
)

sealed interface ProductLookup {
    data class Found(val product: ProductInfo) : ProductLookup
    data object NotFound : ProductLookup
    data class Failed(val message: String) : ProductLookup
}

// looks a barcode up on Open Food Facts. the result only prefills the form, nothing is saved from here
class ProductRepository(private val api: OpenFoodFactsApi = RetrofitClient.api) {

    suspend fun lookup(barcode: String): ProductLookup = try {
        val response = api.getProduct(barcode)
        val product = response.body()?.product
        when {
            response.isSuccessful && product != null -> ProductLookup.Found(product.toInfo(barcode))
            response.isSuccessful || response.code() == 404 -> ProductLookup.NotFound
            else -> ProductLookup.Failed(
                "Open Food Facts didn't respond properly (error ${response.code()}). Try again in a moment."
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ProductLookup.Failed("Couldn't reach Open Food Facts. Check your connection and try again.")
    } catch (e: Exception) {
        ProductLookup.Failed("That product's details couldn't be read. You can still add it by hand.")
    }

    private fun ProductDto.toInfo(barcode: String): ProductInfo {
        val baseName = listOf(productName, productNameEn, genericName)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            .orEmpty()
        return ProductInfo(
            barcode = barcode,
            // the form caps names at 60 characters and Firestore rejects anything over 100,
            // so a long name is trimmed here rather than stalling the sync batch it lands in
            name = withQuantity(baseName, quantity).take(60).trim(),
            brand = brandName(),
            imageUrl = listOf(imageFrontSmallUrl, imageFrontUrl, imageSmallUrl, imageUrl)
                .firstOrNull { !it.isNullOrBlank() },
            category = categoryFrom(categoriesTags.orEmpty())
        )
    }

    // "Greek Yoghurt" plus "500 g" reads better as one name, unless the size is already in it
    private fun withQuantity(name: String, quantity: String?): String {
        val size = quantity?.trim().orEmpty()
        if (name.isBlank() || size.isBlank()) return name
        val squash = { text: String -> text.lowercase().replace(" ", "") }
        return if (squash(name).contains(squash(size))) name else "$name $size"
    }

    private fun ProductDto.brandName(): String? {
        val raw = brands
        val fromField = when {
            raw == null || raw.isJsonNull -> null
            raw.isJsonPrimitive -> raw.asString
            raw.isJsonArray && raw.asJsonArray.size() > 0 -> raw.asJsonArray[0].asString
            else -> null
        }
        val first = fromField?.split(",")?.firstOrNull()?.trim()
        if (!first.isNullOrBlank()) return first
        // tags look like "xx:chobani", so drop the prefix and tidy the case
        return brandsTags?.firstOrNull()
            ?.substringAfter(':')
            ?.replace('-', ' ')
            ?.split(' ')
            ?.joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            ?.ifBlank { null }
    }

    // maps Open Food Facts category tags onto the app's eight categories. the first match wins,
    // and anything unclear is left blank for the user to choose rather than guessed
    private fun categoryFrom(tags: List<String>): FoodCategory? {
        val names = tags.map { it.substringAfter(':') }
        fun has(vararg keys: String) = names.any { tag -> keys.any { key -> tag.contains(key) } }
        return when {
            has("frozen") -> FoodCategory.FROZEN
            has("canned", "tinned") -> FoodCategory.DRY_GOODS
            has("dairies", "dairy", "yogurts", "yoghurts", "cheeses") -> FoodCategory.DAIRY
            has("meats", "poultry", "chicken", "beef", "pork", "lamb", "sausages", "hams", "fishes", "seafood", "salmon", "prawns") ->
                FoodCategory.MEAT_SEAFOOD
            has("breads", "bakery", "pastries", "cakes", "croissants", "muffins", "wraps") -> FoodCategory.BAKERY
            has("sauces", "condiments", "dressings", "spreads", "jams", "mayonnaises", "ketchup", "pestos", "dips") ->
                FoodCategory.PANTRY_SAUCES
            has("pastas", "rices", "cereals", "flours", "legumes", "noodles", "biscuits", "snacks", "crackers", "nuts", "oats", "chocolates") ->
                FoodCategory.DRY_GOODS
            has("fruits", "vegetables", "salads", "herbs") -> FoodCategory.FRUIT_VEG
            else -> null
        }
    }
}
