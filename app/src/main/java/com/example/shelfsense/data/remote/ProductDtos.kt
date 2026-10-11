package com.example.shelfsense.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Gson ignores Kotlin defaults and non-null types, so every field is nullable

// only the product object is read, Gson skips the rest of the envelope
data class ProductResponse(
    @SerializedName("product") val product: ProductDto?
)

data class ProductDto(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("product_name_en") val productNameEn: String?,
    @SerializedName("generic_name") val genericName: String?,
    // a plain string in v3.0, but later schema versions changed it, so it's read loosely
    @SerializedName("brands") val brands: JsonElement?,
    @SerializedName("brands_tags") val brandsTags: List<String>?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("categories_tags") val categoriesTags: List<String>?,
    @SerializedName("image_front_small_url") val imageFrontSmallUrl: String?,
    @SerializedName("image_front_url") val imageFrontUrl: String?,
    @SerializedName("image_small_url") val imageSmallUrl: String?,
    @SerializedName("image_url") val imageUrl: String?
)
