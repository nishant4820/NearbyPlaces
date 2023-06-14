package com.nishant4820.nearbyplaces.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nishant4820.nearbyplaces.BuildConfig
import com.nishant4820.nearbyplaces.R
import com.nishant4820.nearbyplaces.data.NearbyPlacesResponse
import com.nishant4820.nearbyplaces.data.Result
import com.nishant4820.nearbyplaces.databinding.ActivityMapsBinding
import com.nishant4820.nearbyplaces.network.NetworkResult
import com.nishant4820.nearbyplaces.network.PlacesApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: ActivityMapsBinding
    private val defaultLocation = LatLng(28.6128566, 77.2289062)
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private val bottomSheetView by lazy { findViewById<ConstraintLayout>(R.id.bottomSheet) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        setBottomSheetVisibility(false)

        binding.searchNearbyButton.setOnClickListener {
            searchNearbyPlaces()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener { setBottomSheetVisibility(false) }

        getLocationPermission()
        updateLocationUI()
        getDeviceLocation()
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }


    private fun searchNearbyPlaces() {
        if (map == null) {
            return
        }
        if (locationPermissionGranted) {
            requestNearbyPlacesData()
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")

            // Add a default marker, because the user hasn't selected a place.
            map?.addMarker(
                MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(defaultLocation)
                    .snippet(getString(R.string.default_info_snippet))
            )
            getLocationPermission()
        }
    }


    private fun requestNearbyPlacesData() {
        if (lastKnownLocation == null) {
            return
        }
        val parameters: MutableMap<String, String> = mutableMapOf<String, String>().apply {
            put("location", "${lastKnownLocation!!.latitude},${lastKnownLocation!!.longitude}")
            put("radius", "10000")
            put("type", "point_of_interest")
            put("key", BuildConfig.MAPS_API_KEY)
        }
        mainViewModel.getNearbyPlaces(parameters)
        mainViewModel.nearbyPlacesResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Success -> {
                    response.data?.let { setMarkers(it) }
                }
                is NetworkResult.Error -> {
                    Toast.makeText(
                        this,
                        response.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is NetworkResult.Loading -> {

                }
            }
        }
    }

    private fun setMarkers(nearbyPlacesResponse: NearbyPlacesResponse) {
        map?.clear()
        if (nearbyPlacesResponse.status != "OK") {
            Toast.makeText(this, nearbyPlacesResponse.status, Toast.LENGTH_SHORT).show()
            return
        }
        for (place in nearbyPlacesResponse.results) {
            val location = place.geometry?.location ?: continue
            val latLng = LatLng(location.lat, location.lng)
            val mark: Marker? = map?.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(latLng)
            )
            mark?.tag = place.placeId
            map?.setOnMarkerClickListener { marker ->
                for (placeItem in nearbyPlacesResponse.results) {
                    if (marker.tag == placeItem.placeId) {
                        showBottomSheetDialog(placeItem)
                    }
                }
                false
            }
            map?.moveCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        LatLng(
                            lastKnownLocation!!.latitude,
                            lastKnownLocation!!.longitude
                        ), NEARBY_PLACES_ZOOM.toFloat()
                    )
            )
        }
    }

    private fun showBottomSheetDialog(place: Result) {
        bottomSheetView.findViewById<TextView>(R.id.placeName).text = place.name ?: "Not Available"
        bottomSheetView.findViewById<TextView>(R.id.placeVicinity).text =
            place.vicinity ?: "Not Available"
        bottomSheetView.findViewById<TextView>(R.id.placeRating).text =
            if (place.rating != null && place.userRatingsTotal != null) getString(
                R.string.ratingValue,
                place.rating,
                place.userRatingsTotal
            ) else "Rating not Available"
        val openStatusTextView = bottomSheetView.findViewById<TextView>(R.id.placeOpenStatus)
        if (place.openingHours?.openNow != null) {
            if (place.openingHours.openNow) {
                openStatusTextView.text = getString(R.string.openNow)
                openStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.openColor))
            } else {
                openStatusTextView.text = getString(R.string.closed)
                openStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.closedColor))
            }
        } else {
            openStatusTextView.visibility = TextView.INVISIBLE
        }
        val placeIconImageView = bottomSheetView.findViewById<ImageView>(R.id.placeIcon)
        if (place.icon != null)
            Glide.with(this).load(place.icon).into(placeIconImageView)
        if (place.iconBackgroundColor != null)
            placeIconImageView.setBackgroundColor(Color.parseColor(place.iconBackgroundColor))

        bottomSheetView.findViewById<Button>(R.id.directionsButton).setOnClickListener {
            val builder = Uri.Builder()
            builder.scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("dir")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter(
                    "destination",
                    place.geometry?.location?.lat.toString() + "," + place.geometry?.location?.lng.toString()
                )
            val url = builder.build().toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        bottomSheetView.findViewById<Button>(R.id.moreInfoButton).setOnClickListener {
            val builder = Uri.Builder()
            builder.scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("search")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter(
                    "query",
                    place.geometry?.location?.lat.toString() + "," + place.geometry?.location?.lng.toString()
                )
                .appendQueryParameter("query_place_id", place.placeId)
            val url = builder.build().toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        setBottomSheetVisibility(true)
    }

    private fun setBottomSheetVisibility(isVisible: Boolean) {
        val updatedState =
            if (isVisible) BottomSheetBehavior.STATE_HALF_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.state = updatedState
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val NEARBY_PLACES_ZOOM = 14
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        private val okHttpClient = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        private val gsonConverterFactory = GsonConverterFactory.create()

        private val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()

        val placesApi: PlacesApi = retrofit.create(PlacesApi::class.java)
    }
}