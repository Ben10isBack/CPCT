package com.example.cpcontesttracker.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response from the Clist API.
 * The API returns a paginated list of objects.
 */
data class ClistResponse(
    @SerializedName("meta")
    val meta: MetaData,
    @SerializedName("objects")
    val contests: List<Contest>
)

data class MetaData(
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("total_count")
    val totalCount: Int
)
