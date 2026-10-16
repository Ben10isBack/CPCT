package com.example.cpcontesttracker.data.api

import com.example.cpcontesttracker.data.model.ClistResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Defines the endpoints for the Clist API.
 */
interface ClistApiService {

    /**
     * Fetches a list of contests from our Vercel Proxy.
     * The proxy handles the API key and caching, so we don't need to send those headers anymore!
     */
    @GET("api/contests")
    suspend fun getContests(): ClistResponse
}
