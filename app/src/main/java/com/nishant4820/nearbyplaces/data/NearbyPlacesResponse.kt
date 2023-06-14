package com.nishant4820.nearbyplaces.data

import com.google.gson.annotations.SerializedName
import com.nishant4820.nearbyplaces.data.Result

data class NearbyPlacesResponse(
    @SerializedName("results")
    val results: List<Result>,
    @SerializedName("status")
    val status: String
)