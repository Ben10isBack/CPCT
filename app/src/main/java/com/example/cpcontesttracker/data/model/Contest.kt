package com.example.cpcontesttracker.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single contest returned by the Clist API.
 */
data class Contest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("event")
    val event: String,          // The name of the contest
    @SerializedName("start")
    val start: String,          // Start time (UTC) usually in "YYYY-MM-DDTHH:MM:SS" format
    @SerializedName("end")
    val end: String,            // End time (UTC)
    @SerializedName("duration")
    val durationSeconds: Long,  // Duration in seconds
    @SerializedName("href")
    val href: String,           // URL to the contest
    @SerializedName("resource")
    val resource: String      // The platform hosting the contest. In v4 API, this is just a string (e.g. "codeforces.com")
)
