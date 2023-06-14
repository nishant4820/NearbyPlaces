package com.nishant4820.nearbyplaces.data

import com.google.gson.annotations.SerializedName

data class OpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean?
)
