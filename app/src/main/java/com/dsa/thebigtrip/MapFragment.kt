package com.dsa.thebigtrip

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var previewCard: View
    private lateinit var previewImage: ImageView
    private lateinit var previewTitle: TextView
    private lateinit var previewDate: TextView
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
        previewDate = view.findViewById(R.id.previewDate)
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

            val postIds = marker.tag as? List<*> ?: return@setOnMarkerClickListener true
            val posts = postIds
                .mapNotNull { postId -> cachedPosts.find { it.id == postId } }

            if (posts.size == 1) {
                showPostPreview(posts.first())
            } else if (posts.size > 1) {
                showClusterChooser(posts)
            }

            true
        }

        enableUserLocation()
    }

    private fun observePostsAndAddMarkers() {
        repository.getAllPosts().observe(viewLifecycleOwner) { posts ->

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val visiblePosts = if (currentUid == null) {
                emptyList()
            } else {
                posts.filter { it.isVisibleTo(currentUid) }
            }

            cachedPosts = visiblePosts

            googleMap.clear()
            val positions = mutableListOf<LatLng>()

            val locationGroups = visiblePosts
                .filter { it.latitude != null && it.longitude != null }
                .groupBy { getLocationGroupKey(it.latitude!!, it.longitude!!) }

            for (group in locationGroups.values) {
                val firstPost = group.first()
                val lat = firstPost.latitude
                val lng = firstPost.longitude

                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    positions.add(position)

                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(getMarkerTitle(group))
                            .snippet(getMarkerSnippet(group))
                    )
                    marker?.tag = group.map { it.id }
                }
            }

            markerPositions = positions
            if (isMapLoaded) {
                moveCameraToPosts()
            }
        }
    }

    private fun getLocationGroupKey(latitude: Double, longitude: Double): String {
        val latKey = (latitude * 1000).roundToInt()
        val lngKey = (longitude * 1000).roundToInt()
        return "$latKey,$lngKey"
    }

    private fun getMarkerTitle(posts: List<Post>): String {
        return if (posts.size == 1) {
            posts.first().caption ?: "Trip"
        } else {
            "${posts.size} posts here"
        }
    }

    private fun getMarkerSnippet(posts: List<Post>): String {
        return if (posts.size == 1) {
            posts.first().locationName ?: ""
        } else {
            "Tap to choose a post"
        }
    }

    private fun showClusterChooser(posts: List<Post>) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundResource(R.drawable.bg_cluster_dialog)
        }

        val title = TextView(context).apply {
            text = "${posts.size} posts here"
            setTextColor(Color.BLACK)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(title)

        val subtitle = TextView(context).apply {
            text = "Choose which trip you want to preview"
            setTextColor(Color.rgb(102, 102, 102))
            textSize = 13f
            setPadding(0, dp(4), 0, dp(12))
        }
        container.addView(subtitle)

        val rowsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(context).apply {
            addView(rowsContainer)
        }
        container.addView(scrollView)

        val dialog = AlertDialog.Builder(context)
            .setView(container)
            .create()

        posts.forEach { post ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundResource(R.drawable.bg_cluster_item)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    showPostPreview(post)
                }
            }

            val rowTitle = TextView(context).apply {
                text = post.caption ?: "Trip"
                setTextColor(Color.BLACK)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }
            row.addView(rowTitle)

            val rowDate = TextView(context).apply {
                text = formatPostDate(post.createdAt)
                setTextColor(Color.rgb(46, 125, 50))
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
            }
            row.addView(rowDate)

            val rowLocation = TextView(context).apply {
                text = post.locationName ?: "Unknown location"
                setTextColor(Color.rgb(102, 102, 102))
                textSize = 13f
                setPadding(0, dp(5), 0, 0)
            }
            row.addView(rowLocation)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            rowsContainer.addView(row, params)
        }

        dialog.show()
    }

    private fun showPostPreview(post: Post) {
        previewTitle.text = post.caption ?: ""
        previewDate.text = formatPostDate(post.createdAt)
        previewLocation.text = post.locationName ?: ""

        if (!post.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(post.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .into(previewImage)
        } else {
            previewImage.setImageResource(R.drawable.bg_image_placeholder)
        }

        previewCard.visibility = View.VISIBLE
    }

    private fun formatPostDate(createdAt: Long): String {
        if (createdAt <= 0) {
            return "Published date unknown"
        }

        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "Published ${formatter.format(Date(createdAt))}"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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
