package com.nishant4820.nearbyplaces.network

import com.nishant4820.nearbyplaces.data.NearbyPlacesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface PlacesApi {

    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @QueryMap parameters: Map<String, String>
    ): Response<NearbyPlacesResponse>
}