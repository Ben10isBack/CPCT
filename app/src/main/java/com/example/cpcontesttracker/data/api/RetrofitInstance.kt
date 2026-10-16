package com.example.cpcontesttracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * A singleton object to provide a single instance of Retrofit throughout the app.
 */
object RetrofitInstance {
    // We are now pointing to our Vercel Proxy instead of directly to Clist!
    private const val BASE_URL = "https://backend-rho-ochre-17.vercel.app/"

    // Interceptor to log network requests and responses in Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // The Retrofit instance
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
