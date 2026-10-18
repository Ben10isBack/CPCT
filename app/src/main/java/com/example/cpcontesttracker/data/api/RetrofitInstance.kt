package com.example.cpcontesttracker.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * A singleton object to provide a single instance of Retrofit throughout the app.
 */
object RetrofitInstance {
    // We are now pointing to our Vercel Proxy instead of directly to Clist!
    private const val BASE_URL = "https://backend-rho-ochre-17.vercel.app/"

    // No BODY logging: dumping the whole JSON response to Logcat on every fetch is slow
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Expose the API service
    val api: ClistApiService by lazy {
        retrofit.create(ClistApiService::class.java)
    }
}
