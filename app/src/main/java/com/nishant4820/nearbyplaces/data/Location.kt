package com.nishant4820.nearbyplaces.data


import com.google.gson.annotations.SerializedName

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)