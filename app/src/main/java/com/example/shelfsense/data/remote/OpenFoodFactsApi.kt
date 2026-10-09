package com.example.shelfsense.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    // pinned to v3 so the response keeps the shape ProductDto expects.
    // an unknown barcode comes back as a 404, which is why this returns Response rather than the body
    @GET("api/v3/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = PRODUCT_FIELDS
    ): Response<ProductResponse>

    companion object {
        // only the fields the form uses, which keeps the response small
        const val PRODUCT_FIELDS = "code,product_name,product_name_en,generic_name,brands,brands_tags," +
            "quantity,categories_tags,image_front_small_url,image_front_url,image_small_url,image_url"
    }
}
