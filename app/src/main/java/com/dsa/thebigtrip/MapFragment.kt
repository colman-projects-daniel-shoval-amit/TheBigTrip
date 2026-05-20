package com.dsa.thebigtrip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var previewCard: View
    private lateinit var previewImage: ImageView
    private lateinit var previewTitle: TextView
    private lateinit var previewLocation: TextView
    private lateinit var closePreview: TextView

    private val repository = PostRepository.shared
    private var cachedPosts: List<Post> = emptyList()
    private var markerPositions: List<LatLng> = emptyList()
    private var isMapLoaded = false

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

        googleMap.setOnMapLoadedCallback {
            isMapLoaded = true
            moveCameraToPosts()
        }

        observePostsAndAddMarkers()
        refreshPosts()

        googleMap.setOnMarkerClickListener { marker ->

            val postId = marker.tag as? String ?: return@setOnMarkerClickListener true

            val post = cachedPosts.find { it.id == postId }

            post?.let {
                previewTitle.text = it.caption ?: ""
                previewLocation.text = it.locationName ?: ""

                if (!it.imageUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(it.imageUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .into(previewImage)
                } else {
                    previewImage.setImageResource(R.drawable.bg_image_placeholder)
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
            val positions = mutableListOf<LatLng>()

            for (post in posts) {
                val lat = post.latitude
                val lng = post.longitude

                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    positions.add(position)

                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(post.caption ?: "Trip")
                            .snippet(post.locationName ?: "")
                    )
                    marker?.tag = post.id
                }
            }

            markerPositions = positions
            if (isMapLoaded) {
                moveCameraToPosts()
            }
        }
    }

    private fun moveCameraToPosts() {
        if (markerPositions.isEmpty()) {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(31.0461, 34.8516), 7f)
            )
            return
        }

        if (markerPositions.size == 1) {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(markerPositions.first(), 10f)
            )
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        markerPositions.forEach { boundsBuilder.include(it) }

        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
        )
    }

    private fun refreshPosts() {
        lifecycleScope.launch {
            try {
                repository.refreshPosts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to refresh map posts", Toast.LENGTH_SHORT).show()
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
    }
}
