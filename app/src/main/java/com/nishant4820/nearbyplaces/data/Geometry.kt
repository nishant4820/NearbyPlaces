package com.nishant4820.nearbyplaces.data


import com.google.gson.annotations.SerializedName

data class Geometry(
    @SerializedName("location")
    val location: Location
)