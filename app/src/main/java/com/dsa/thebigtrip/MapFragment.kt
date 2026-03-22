package com.dsa.thebigtrip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.data.post.PostRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val repository = PostRepository.shared

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
                CameraUpdateFactory.newLatLngZoom(israelCenter, 7f)
            )
        }

        observePostsAndAddMarkers()

        googleMap.setOnMarkerClickListener { marker ->

            val bundle = Bundle()
            bundle.putString("locationName", marker.title)

            findNavController().navigate(
                R.id.profileFragment,
                bundle
            )

            true
        }

        enableUserLocation()
    }

    // 🔥 MAIN LOGIC: observe DB and update markers
    private fun observePostsAndAddMarkers() {
        repository.getAllPosts().observe(viewLifecycleOwner) { posts ->

            googleMap.clear()

            for (post in posts) {

                val lat = post.latitude
                val lng = post.longitude

                if (lat != null && lng != null) {

                    val latLng = LatLng(lat, lng)

                    googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(post.caption ?: "Trip")
                            .snippet(post.locationName ?: "")
                    )
                }
            }

            // 🔥 Move camera to first post
            if (posts.isNotEmpty()) {
                val first = posts.first()
                if (first.latitude != null && first.longitude != null) {
                    val latLng = LatLng(first.latitude!!, first.longitude!!)
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 10f)
                    )
                }
            }
        }
    }

    // 📍 User location
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
        }
    }
}