package com.nishant4820.nearbyplaces.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nishant4820.nearbyplaces.data.NearbyPlacesResponse
import com.nishant4820.nearbyplaces.network.NetworkResult
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    var nearbyPlacesResponse: MutableLiveData<NetworkResult<NearbyPlacesResponse>> =
        MutableLiveData()

    fun getNearbyPlaces(parameters: Map<String, String>) = viewModelScope.launch {
        getNearbyPlacesSafeCall(parameters)
    }

    private suspend fun getNearbyPlacesSafeCall(parameters: Map<String, String>) {
        nearbyPlacesResponse.value = NetworkResult.Loading()
        if (hasInternetConnection()) {
            try {
                val response = MapsActivity.placesApi.getNearbyPlaces(parameters)
                nearbyPlacesResponse.value = handleNearbyPlacesResponse(response)
            } catch (e: Exception) {
                nearbyPlacesResponse.value =
                    NetworkResult.Error("Something went wrong. Please try again later!")
            }
        } else {
            nearbyPlacesResponse.value = NetworkResult.Error("No Internet Connection.")
        }
    }

    private fun handleNearbyPlacesResponse(response: Response<NearbyPlacesResponse>): NetworkResult<NearbyPlacesResponse> {
        return when {
            response.message().toString().contains("timeout") -> {
                NetworkResult.Error("Timeout")
            }
            response.isSuccessful -> {
                val nearbyPlaces = response.body()
                NetworkResult.Success(nearbyPlaces!!)
            }
            else -> {
                NetworkResult.Error(response.message())
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}