package com.dsa.thebigtrip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.data.repository.posts.PostRepository
import com.dsa.thebigtrip.map.ClusterPostsBottomSheetFragment
import com.dsa.thebigtrip.map.PostClusterItem
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<PostClusterItem>
    private var savedCameraPosition: CameraPosition? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLocationPermissionGranted()
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // "Don't ask again" was selected — guide user to system settings.
            Snackbar.make(
                requireView(),
                "Location permission is disabled. Enable it in Settings to center the map.",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }.show()
        }
        // else: plain denial — user can tap the map's my-location button or re-open the tab.
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        clusterManager = ClusterManager(requireContext(), googleMap)
        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterClickListener { cluster ->
            savedCameraPosition = googleMap.cameraPosition
            ClusterPostsBottomSheetFragment
                .newInstance(cluster.items.map { it.post })
                .show(childFragmentManager, "cluster_posts")
            true
        }

        clusterManager.setOnClusterItemClickListener { item ->
            savedCameraPosition = googleMap.cameraPosition
            findNavController().navigate(
                R.id.action_mapFragment_to_postDetailsFragment,
                Bundle().apply { putString("postId", item.post.id) }
            )
            true
        }

        val saved = savedCameraPosition
        if (saved != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(saved))
        } else {
            enableUserLocation()
        }
        loadPostMarkers()
    }

    private fun loadPostMarkers() {
        lifecycleScope.launch {
            val items = PostRepository.shared.getAllPosts().map { PostClusterItem(it) }
            clusterManager.addItems(items)
            clusterManager.cluster()
        }
    }

    private fun enableUserLocation() {
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onLocationPermissionGranted()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Denied once — explain why before asking again.
                Snackbar.make(
                    requireView(),
                    "Location access centers the map on your current position",
                    Snackbar.LENGTH_LONG
                ).setAction("Allow") {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }

            else -> {
                // First-time request (or system already cleared the denial state).
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun onLocationPermissionGranted() {
        if (!::googleMap.isInitialized) return
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        googleMap.isMyLocationEnabled = true

        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())
        val cts = CancellationTokenSource()

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (!isAdded) return@addOnSuccessListener
                if (location != null) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
                    )
                } else {
                    // null = GPS hardware couldn't produce a fix; try the cached lastLocation.
                    fallbackToLastLocation(fused)
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                fallbackToLastLocation(fused)
            }
    }

    private fun fallbackToLastLocation(fused: com.google.android.gms.location.FusedLocationProviderClient) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fused.lastLocation
            .addOnSuccessListener { last ->
                if (!isAdded) return@addOnSuccessListener
                if (last != null) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 15f)
                    )
                } else {
                    showGpsUnavailableSnackbar()
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                showGpsUnavailableSnackbar()
            }
    }

    private fun showGpsUnavailableSnackbar() {
        if (!isAdded) return
        Snackbar.make(
            requireView(),
            "Unable to establish GPS fix. Please verify device location settings.",
            Snackbar.LENGTH_LONG
        ).show()
    }
}
