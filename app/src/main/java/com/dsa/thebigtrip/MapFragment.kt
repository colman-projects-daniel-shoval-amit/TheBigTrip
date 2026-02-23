package com.dsa.thebigtrip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.navigation.fragment.findNavController

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    //TODO: Take real locations from db of posts
    private val locations = listOf(
        LatLng(32.0853, 34.7818), // Tel Aviv
        LatLng(31.7683, 35.2137), // Jerusalem
        LatLng(32.7940, 34.9896)  // Haifa
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val israelCenter = LatLng(31.0461, 34.8516)

        googleMap.setOnMapLoadedCallback {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(israelCenter, 8f)
            )
        }

        addCustomMarkers()
        googleMap.setOnMarkerClickListener { marker ->

            val bundle = Bundle()
            bundle.putString("locationName", marker.title)
            //TODO: Here put the fragment to move or w.e you want to do when the market is clicked
            findNavController().navigate(
                R.id.profileFragment,
                bundle
            )

            true
        }
        enableUserLocation()
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        googleMap.isMyLocationEnabled = true

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 12f)
                )
            }
            // If location is null stays on Israel
        }
    }

    private fun addCustomMarkers() {
        for (latLng in locations) {
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Trip Location")
            )
        }
    }
}