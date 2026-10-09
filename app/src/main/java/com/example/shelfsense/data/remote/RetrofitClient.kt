package com.example.shelfsense.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// one Retrofit instance for the app, created the first time a lookup runs
object RetrofitClient {

    private const val BASE_URL = "https://world.openfoodfacts.org/"

    // Open Food Facts asks every app to identify itself as AppName/Version (contact)
    private const val USER_AGENT = "ShelfSense/1.0 (nbat0021@student.monash.edu)"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
