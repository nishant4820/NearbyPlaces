package com.nishant4820.nearbyplaces.data

import com.google.gson.annotations.SerializedName
import com.nishant4820.nearbyplaces.data.Geometry

data class Result(
    @SerializedName("geometry")
    val geometry: Geometry?,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("icon_background_color")
    val iconBackgroundColor: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("opening_hours")
    val openingHours: OpeningHours?,
    @SerializedName("place_id")
    val placeId: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("types")
    val types: List<String>?,
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int?,
    @SerializedName("vicinity")
    val vicinity: String?
)