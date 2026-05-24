package com.dsa.thebigtrip.Post

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.dsa.thebigtrip.utils.ImageUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val repository = PostRepository.shared
    private val userRepository = UserRepository.shared

    private var selectedImageUri: Uri? = null
    private var currentPost: Post? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedPlaceName: String? = null
    private var suppressLocationSearch = false

    private var allUsers: List<User> = emptyList()
    private val selectedVisibleUserIds: MutableSet<String> = mutableSetOf()
    private var autocompleteSessionToken: AutocompleteSessionToken? = null
    private var locationPredictions: List<AutocompletePrediction> = emptyList()
    private var locationSearchRequestId = 0
    private lateinit var placesClient: PlacesClient

    // 📸 Image picker with preview
    private val imagePickerLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                selectedImageUri = it
                showImagePreview(it.toString())
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val hasLocationPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (hasLocationPermission) {
                fillCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializePlaces()
        setupListeners()
        loadAllUsers()
        loadPostForEditIfNeeded()
        updatePermissionsSummary()
    }

    private fun setupListeners() {

        // 📸 Open gallery
        binding.imageUploadContainer.setOnClickListener {
            hideLocationSuggestions()
            openGallery()
        }

        // 🚀 Publish
        binding.currentLocationButton.setOnClickListener {
            binding.locationInput.clearFocus()
            hideLocationSuggestions()
            fillCurrentLocation()
        }

        binding.locationInput.setOnClickListener {
            binding.locationInput.requestFocus()
            showKeyboard(binding.locationInput)
            if (locationPredictions.isNotEmpty()) {
                showLocationSuggestions(locationPredictions)
            }
        }

        binding.locationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressLocationSearch) {
                    return
                }

                selectedLatitude = null
                selectedLongitude = null
                selectedPlaceName = null

                val query = s?.toString()?.trim().orEmpty()
                if (query.length >= 2) {
                    searchPlaces(query)
                } else {
                    hideLocationSuggestions()
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.publishButton.setOnClickListener {
            hideLocationSuggestions()
            val title = binding.titleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()

            if (validateInput(title)) {
                savePost(title, description)
            }
        }

        binding.selectPermissionsButton.setOnClickListener {
            hideLocationSuggestions()
            showPermissionPicker()
        }

        binding.titleInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideLocationSuggestions()
            }
        }

        binding.descriptionInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideLocationSuggestions()
            }
        }
    }

    private fun initializePlaces() {
        if (Places.isInitialized()) {
            placesClient = Places.createClient(requireContext())
            return
        }

        val apiKey = getMapsApiKey()

        if (apiKey.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Google Places API key is missing", Toast.LENGTH_SHORT).show()
            return
        }

        Places.initialize(requireContext().applicationContext, apiKey)
        placesClient = Places.createClient(requireContext())
    }

    private fun getMapsApiKey(): String? {
        val appInfo = requireContext().packageManager.getApplicationInfo(
            requireContext().packageName,
            PackageManager.GET_META_DATA
        )

        return appInfo.metaData?.getString("com.google.android.geo.API_KEY")
    }

    private fun setSelectedPlace(place: Place) {
        val latLng = place.latLng

        if (latLng == null) {
            Toast.makeText(requireContext(), "Selected place has no location", Toast.LENGTH_SHORT).show()
            return
        }

        selectedLatitude = latLng.latitude
        selectedLongitude = latLng.longitude
        selectedPlaceName = place.name ?: place.address ?: "Selected place"
        setLocationText(selectedPlaceName.orEmpty())
        binding.locationInput.error = null
        hideLocationSuggestions()
        autocompleteSessionToken = null
    }

    private fun searchPlaces(query: String) {
        if (!hasInternetConnection()) {
            showLocationMessage("Check your internet connection")
            return
        }

        if (!Places.isInitialized() || !::placesClient.isInitialized) {
            showLocationMessage("Google Places is not ready")
            return
        }

        if (autocompleteSessionToken == null) {
            autocompleteSessionToken = AutocompleteSessionToken.newInstance()
        }

        val requestId = ++locationSearchRequestId
        showLocationMessage("Searching...")

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(autocompleteSessionToken)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (requestId != locationSearchRequestId) {
                    return@addOnSuccessListener
                }
                showLocationSuggestions(response.autocompletePredictions.take(5))
            }
            .addOnFailureListener { exception ->
                if (requestId != locationSearchRequestId) {
                    return@addOnFailureListener
                }
                val message = getPlaceSearchErrorMessage(exception)
                showLocationMessage(message)
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = ContextCompat.getSystemService(
            requireContext(),
            ConnectivityManager::class.java
        ) ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getPlaceSearchErrorMessage(exception: Exception): String {
        val rawMessage = exception.message.orEmpty()

        return if (
            rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
            rawMessage.contains("No address associated with hostname", ignoreCase = true)
        ) {
            "Cannot reach Google Places. Check emulator/device internet or DNS."
        } else {
            rawMessage.ifBlank { "Place suggestions failed" }
        }
    }

    private fun showLocationSuggestions(predictions: List<AutocompletePrediction>) {
        val currentBinding = _binding ?: return
        hideLocationSuggestions()

        if (predictions.isEmpty()) {
            return
        }

        locationPredictions = predictions

        if (!currentBinding.locationInput.hasFocus()) {
            return
        }

        val suggestions = currentBinding.locationSuggestionsPanel

        predictions.forEach { prediction ->
            suggestions.addView(createLocationSuggestionRow(prediction))
        }
        suggestions.visibility = View.VISIBLE
    }

    private fun createLocationSuggestionRow(prediction: AutocompletePrediction): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dpToPx(), 10.dpToPx(), 14.dpToPx(), 10.dpToPx())
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_cluster_item)
            setOnClickListener {
                selectPlacePrediction(prediction)
            }
        }

        row.addView(
            TextView(requireContext()).apply {
                text = prediction.getPrimaryText(null).toString()
                setTextColor(Color.BLACK)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        val secondaryText = prediction.getSecondaryText(null).toString()
        if (secondaryText.isNotBlank()) {
            row.addView(
                TextView(requireContext()).apply {
                    text = secondaryText
                    setTextColor("#5F6F66".toColorInt())
                    textSize = 13f
                }
            )
        }

        return row
    }

    private fun selectPlacePrediction(prediction: AutocompletePrediction) {
        if (!::placesClient.isInitialized) {
            return
        }

        val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(prediction.placeId, fields)
            .setSessionToken(autocompleteSessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                setSelectedPlace(response.place)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Place search failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideLocationSuggestions() {
        val currentBinding = _binding ?: return
        currentBinding.locationSuggestionsPanel.removeAllViews()
        currentBinding.locationSuggestionsPanel.visibility = View.GONE
    }

    private fun showLocationMessage(message: String) {
        val currentBinding = _binding ?: return
        val suggestions = currentBinding.locationSuggestionsPanel

        suggestions.removeAllViews()
        suggestions.addView(
            TextView(requireContext()).apply {
                text = message
                setTextColor("#5F6F66".toColorInt())
                textSize = 14f
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            }
        )
        suggestions.visibility = View.VISIBLE
    }

    private fun setLocationText(text: String) {
        val currentBinding = _binding ?: return
        suppressLocationSearch = true
        currentBinding.locationInput.setText(text)
        currentBinding.locationInput.setSelection(text.length)
        suppressLocationSearch = false
    }

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showKeyboard(view: View) {
        val inputMethodManager = ContextCompat.getSystemService(
            requireContext(),
            InputMethodManager::class.java
        )
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun fillCurrentLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        val priority = if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        fusedLocationClient
            .getCurrentLocation(priority, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    setCurrentLocation(location.latitude, location.longitude)
                } else {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                setCurrentLocation(lastLocation.latitude, lastLocation.longitude)
                            } else {
                                Toast.makeText(requireContext(), "Could not get current location", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Could not get current location", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Could not get current location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setCurrentLocation(latitude: Double, longitude: Double) {
        val currentBinding = _binding ?: return
        selectedLatitude = latitude
        selectedLongitude = longitude
        selectedPlaceName = "Current location"
        setLocationText(selectedPlaceName.orEmpty())
        currentBinding.locationInput.error = null
        hideLocationSuggestions()
    }

    private fun loadPostForEditIfNeeded() {
        val postId = arguments?.getString("postId") ?: return

        setLoading(true)

        lifecycleScope.launch {
            try {
                val post = repository.getPostById(postId) ?: return@launch
                currentPost = post

                binding.createPostMainTitle.text = "Edit Post"
                binding.publishButton.text = "Save Changes"
                binding.titleInput.setText(post.caption ?: "")
                binding.descriptionInput.setText(post.locationName ?: "")

                if (post.latitude != null && post.longitude != null) {
                    selectedLatitude = post.latitude
                    selectedLongitude = post.longitude
                    selectedPlaceName = post.locationName ?: "Selected place"
                    setLocationText(selectedPlaceName.orEmpty())
                }

                if (!post.imageUrl.isNullOrEmpty()) {
                    showImagePreview(post.imageUrl)
                }

                selectedVisibleUserIds.clear()
                selectedVisibleUserIds.addAll(post.visibleTo)
                updatePermissionsSummary()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to load post", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#ff4545".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validateInput(title: String): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            isValid = false
        } else {
            binding.titleInput.error = null
        }

        if (selectedLatitude == null || selectedLongitude == null || selectedPlaceName.isNullOrBlank()) {
            binding.locationInput.error = "Choose a location from suggestions"
            isValid = false
        } else {
            binding.locationInput.error = null
        }

        return isValid
    }

    private fun savePost(
        title: String,
        description: String
    ) {
        if (currentPost == null) {
            publishPost(title, description)
        } else {
            updatePost(title, description)
        }
    }

    private fun publishPost(
        title: String,
        description: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("Please log in before publishing")
                val lat = selectedLatitude ?: throw IllegalStateException("Location is required")
                val lng = selectedLongitude ?: throw IllegalStateException("Location is required")
                val postId = System.currentTimeMillis().toString()
                val imageUrl = selectedImageUri?.let {
                    ImageUtil.uploadPostImage(requireContext(), it, postId)
                        ?: throw IllegalStateException("Failed to upload image")
                }

                val post = Post(
                    id = postId,
                    userId = uid,
                    caption = title,
                    imageUrl = imageUrl,
                    createdAt = System.currentTimeMillis(),
                    locationName = selectedPlaceName ?: description,
                    latitude = lat,
                    longitude = lng,
                    visibleTo = selectedVisibleUserIds.filter { it != uid }
                )

                repository.addPost(post)

                Snackbar.make(binding.root, "Post published!", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#4CAF50".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()

                setLoading(false)
                requireActivity().onBackPressedDispatcher.onBackPressed()

            } catch (e: Exception) {
                Snackbar.make(binding.root, e.message ?: "Error", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#ff4545".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updatePost(
        title: String,
        description: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val existingPost = currentPost ?: return@launch
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("Please log in before editing")

                if (existingPost.userId != uid) {
                    throw IllegalStateException("You can edit only your own posts")
                }

                val lat = selectedLatitude ?: throw IllegalStateException("Location is required")
                val lng = selectedLongitude ?: throw IllegalStateException("Location is required")
                val imageUrl = selectedImageUri?.let {
                    ImageUtil.uploadPostImage(requireContext(), it, existingPost.id)
                        ?: throw IllegalStateException("Failed to upload image")
                } ?: existingPost.imageUrl

                val updatedPost = existingPost.copy(
                    caption = title,
                    imageUrl = imageUrl,
                    locationName = selectedPlaceName ?: description,
                    latitude = lat,
                    longitude = lng,
                    visibleTo = selectedVisibleUserIds.filter { it != existingPost.userId }
                )

                repository.updatePost(updatedPost)

                Snackbar.make(binding.root, "Post updated!", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#4CAF50".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()

                setLoading(false)
                requireActivity().onBackPressedDispatcher.onBackPressed()

            } catch (e: Exception) {
                Snackbar.make(binding.root, e.message ?: "Error", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#ff4545".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showImagePreview(imageUrl: String) {
        binding.imageUploadTextview.visibility = View.GONE

        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        binding.imageUploadContainer.removeAllViews()
        binding.imageUploadContainer.addView(imageView)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .error(R.drawable.bg_image_placeholder)
            .into(imageView)
    }

    private fun setLoading(isLoading: Boolean) {
        val currentBinding = _binding ?: return

        currentBinding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        currentBinding.publishButton.isEnabled = !isLoading
        currentBinding.descriptionInput.isEnabled = !isLoading
        currentBinding.titleInput.isEnabled = !isLoading
        currentBinding.locationInput.isEnabled = !isLoading
        currentBinding.selectPermissionsButton.isEnabled = !isLoading
    }

    private fun loadAllUsers() {
        lifecycleScope.launch {
            try {
                allUsers = userRepository.getAllUsers()
            } catch (e: Exception) {
                allUsers = emptyList()
            }
        }
    }

    private fun showPermissionPicker() {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        val candidates = allUsers.filter { it.uid != ownerUid }

        if (candidates.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No other users available to share with",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val labels = candidates.map { user ->
            user.fullName?.takeIf { it.isNotBlank() }
                ?: user.email?.takeIf { it.isNotBlank() }
                ?: user.uid
        }.toTypedArray()

        val checked = BooleanArray(candidates.size) { index ->
            selectedVisibleUserIds.contains(candidates[index].uid)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Who can view this post")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val uid = candidates[which].uid
                if (isChecked) {
                    selectedVisibleUserIds.add(uid)
                } else {
                    selectedVisibleUserIds.remove(uid)
                }
            }
            .setPositiveButton("Done") { _, _ ->
                updatePermissionsSummary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePermissionsSummary() {
        val currentBinding = _binding ?: return
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        val sharedIds = selectedVisibleUserIds.filter { it != ownerUid }

        currentBinding.permissionsSummary.text = when {
            sharedIds.isEmpty() -> "Only you"
            sharedIds.size == 1 -> "Shared with 1 user"
            else -> "Shared with ${sharedIds.size} users"
        }
    }

    override fun onDestroyView() {
        hideLocationSuggestions()
        super.onDestroyView()
        _binding = null
    }
}
