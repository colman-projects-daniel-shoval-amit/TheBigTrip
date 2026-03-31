package com.dsa.thebigtrip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var previewCard: View
    private lateinit var previewImage: ImageView
    private lateinit var previewTitle: TextView
    private lateinit var previewLocation: TextView
    private lateinit var closePreview: TextView

    private val repository = PostRepository.shared
    private var cachedPosts: List<Post> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        previewCard = view.findViewById(R.id.postPreviewCard)
        previewImage = view.findViewById(R.id.previewImage)
        previewTitle = view.findViewById(R.id.previewTitle)
        previewLocation = view.findViewById(R.id.previewLocation)
        closePreview = view.findViewById(R.id.closePreview)

        closePreview.setOnClickListener {
            previewCard.visibility = View.GONE
        }

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

            val postId = marker.tag as? String ?: return@setOnMarkerClickListener true

            val post = cachedPosts.find { it.id == postId }

            post?.let {
                previewTitle.text = it.caption ?: ""
                previewLocation.text = it.locationName ?: ""

                if (!it.imageUrl.isNullOrEmpty()) {
                    Picasso.get().load(it.imageUrl).into(previewImage)
                } else {
                    previewImage.setImageDrawable(null)
                }

                previewCard.visibility = View.VISIBLE
            }

            true
        }

        enableUserLocation()
    }

    private fun observePostsAndAddMarkers() {
        repository.getAllPosts().observe(viewLifecycleOwner) { posts ->

            cachedPosts = posts

            googleMap.clear()

            for (post in posts) {
                val lat = post.latitude
                val lng = post.longitude

                if (lat != null && lng != null) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(post.caption ?: "Trip")
                            .snippet(post.locationName ?: "")
                    )
                    marker?.tag = post.id
                }
            }

            if (posts.isNotEmpty()) {
                val first = posts.first()
                if (first.latitude != null && first.longitude != null) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(first.latitude!!, first.longitude!!),
                            10f
                        )
                    )
                }
            }
        }
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
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        12f
                    )
                )
            }
        }
    }
}