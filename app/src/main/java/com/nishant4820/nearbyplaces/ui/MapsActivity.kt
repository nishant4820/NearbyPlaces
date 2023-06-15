package com.nishant4820.nearbyplaces.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.nishant4820.nearbyplaces.BuildConfig
import com.nishant4820.nearbyplaces.R
import com.nishant4820.nearbyplaces.data.NearbyPlacesResponse
import com.nishant4820.nearbyplaces.databinding.ActivityMapsBinding
import com.nishant4820.nearbyplaces.network.NetworkResult
import com.nishant4820.nearbyplaces.network.PlacesApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var searchNearbyFAB: ExtendedFloatingActionButton
    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: ActivityMapsBinding
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private val bottomSheetView by lazy { findViewById<ConstraintLayout>(R.id.bottomSheet) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchNearbyFAB = binding.searchNearbyButton
        searchNearbyFAB.setOnClickListener {
            searchNearbyPlaces("point_of_interest")
        }
        binding.restaurantButton.setOnClickListener { searchNearbyPlaces("restaurant") }
        binding.petrolButton.setOnClickListener { searchNearbyPlaces("gas_station") }
        binding.hotelsButton.setOnClickListener { searchNearbyPlaces("lodging") }
        binding.coffeeButton.setOnClickListener { searchNearbyPlaces("cafe") }
        binding.shoppingButton.setOnClickListener { searchNearbyPlaces("shopping_mall") }
        binding.hospitalsClinicsButton.setOnClickListener { searchNearbyPlaces("hospital") }

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(placeFields)
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
                Toast.makeText(this@MapsActivity, "An error occurred: $status", Toast.LENGTH_LONG)
                    .show()
            }

            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
                map?.clear()
                if (place.latLng != null) {
                    val mark: Marker? = map?.addMarker(
                        MarkerOptions()
                            .title(place.name)
                            .position(place.latLng!!)
                    )
                    mark?.tag = place.id
                    map?.animateCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(place.latLng!!, DEFAULT_ZOOM.toFloat())
                    )
                }
                showBottomSheetDialog(place)

            }

        })

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        setBottomSheetVisibility(false)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener {
            map?.clear()
            autocompleteFragment.setText("")
            setBottomSheetVisibility(false)
        }

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
                            map?.uiSettings?.isMyLocationButtonEnabled = false
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        Toast.makeText(this, "Location Unavailable!", Toast.LENGTH_SHORT).show()
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


    private fun searchNearbyPlaces(type: String) {
        if (map == null) {
            return
        }
        if (locationPermissionGranted) {
            requestNearbyPlacesData(type)
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")
            Toast.makeText(this, "Location Permission not available!", Toast.LENGTH_SHORT).show()
            getLocationPermission()
        }
    }


    private fun requestNearbyPlacesData(type: String) {
        if (lastKnownLocation == null) {
            return
        }
        val parameters: MutableMap<String, String> = mutableMapOf<String, String>().apply {
            put("location", "${lastKnownLocation!!.latitude},${lastKnownLocation!!.longitude}")
            put("radius", "10000")
            put("type", type)
            put("key", BuildConfig.MAPS_API_KEY)
        }
        mainViewModel.getNearbyPlaces(parameters)
        mainViewModel.nearbyPlacesResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Success -> {
                    response.data?.let {
                        setBottomSheetVisibility(false)
                        setMarkers(it)
                    }
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
        val builder = LatLngBounds.Builder()
        for (place in nearbyPlacesResponse.results) {
            val location = place.geometry?.location ?: continue
            val latLng = LatLng(location.lat, location.lng)
            val mark: Marker? = map?.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(latLng)
            )
            mark?.tag = place.placeId
            builder.include(latLng)
        }
        val bounds = builder.build()
        val paddingFromEdgeAsPX = 100
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingFromEdgeAsPX))

        map?.setOnMarkerClickListener { marker ->
            //Get PlaceID from TAG
            val placeId = marker.tag as String?
            // Construct a request object, passing the place ID and fields array.
            if (placeId != null) {
                val request = FetchPlaceRequest.newInstance(placeId, placeFields)

                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response: FetchPlaceResponse ->
                        val place = response.place
                        showBottomSheetDialog(place)
                        Log.i(TAG, "Place found: ${place.name}")
                    }.addOnFailureListener { exception: Exception ->
                        if (exception is ApiException) {
                            Log.e(TAG, "Place not found: ${exception.message}")
                            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            false
        }

    }

    private fun showBottomSheetDialog(place: Place) {

        val isOpenTask: Task<IsOpenResponse> = placesClient.isOpen(IsOpenRequest.newInstance(place))
        isOpenTask.addOnSuccessListener { response ->
            val openStatusTextView = bottomSheetView.findViewById<TextView>(R.id.placeOpenStatus)
            val isOpen = response.isOpen
            if (isOpen != null) {
                if (isOpen) {
                    openStatusTextView.text = getString(R.string.openNow)
                    openStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.openColor))
                } else {
                    openStatusTextView.text = getString(R.string.closed)
                    openStatusTextView.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.closedColor
                        )
                    )
                }
            } else {
                openStatusTextView.visibility = TextView.INVISIBLE
            }
        }
        bottomSheetView.findViewById<TextView>(R.id.placeName).text = place.name ?: "Not Available"
        bottomSheetView.findViewById<TextView>(R.id.placeVicinity).text =
            place.address ?: "Not Available"
        bottomSheetView.findViewById<TextView>(R.id.placeRating).text =
            if (place.rating != null && place.userRatingsTotal != null) getString(
                R.string.ratingValue,
                place.rating,
                place.userRatingsTotal
            ) else "Rating not Available"
        val placeIconImageView = bottomSheetView.findViewById<ImageView>(R.id.placeIcon)
        if (place.iconUrl != null)
            Glide.with(this).load(place.iconUrl).into(placeIconImageView)
        if (place.iconBackgroundColor != null)
            placeIconImageView.setBackgroundColor(place.iconBackgroundColor!!)

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
                    place.latLng?.latitude.toString() + "," + place.latLng?.longitude.toString()
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
                    place.latLng?.latitude.toString() + "," + place.latLng?.longitude.toString()
                )
                .appendQueryParameter("query_place_id", place.id)
            val url = builder.build().toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        setBottomSheetVisibility(true)
    }

    private fun setBottomSheetVisibility(isVisible: Boolean) {
        val updatedState =
            if (isVisible) {
                searchNearbyFAB.visibility = ExtendedFloatingActionButton.GONE
                BottomSheetBehavior.STATE_HALF_EXPANDED
            } else {
                searchNearbyFAB.visibility = ExtendedFloatingActionButton.VISIBLE
                BottomSheetBehavior.STATE_COLLAPSED
            }
        bottomSheetBehavior.state = updatedState
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
//                map?.uiSettings?.isMyLocationButtonEnabled = true
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
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private val placeFields = listOf(
            Place.Field.ID,
            Place.Field.LAT_LNG,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.RATING,
            Place.Field.USER_RATINGS_TOTAL,
            Place.Field.ICON_URL,
            Place.Field.ICON_BACKGROUND_COLOR
        )
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