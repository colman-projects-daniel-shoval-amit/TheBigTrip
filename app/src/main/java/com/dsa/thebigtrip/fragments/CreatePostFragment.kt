package com.dsa.thebigtrip.fragments

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.repository.posts.PostRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedLatLng: LatLng? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imagePreview.setImageURI(it)
            binding.imagePreview.visibility = View.VISIBLE
            binding.imageUploadTextview.visibility = View.GONE
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchAndFillLocation()
        } else {
            Snackbar.make(
                requireView(),
                "Location permission needed to auto-fill coordinates",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    onPlaceSelected(place)
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Snackbar.make(
                        requireView(),
                        "Search error: ${status.statusMessage}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
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
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.imageUploadContainer.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.locationSearchLayout.setOnClickListener { launchAutocomplete() }
        binding.locationSearchInput.setOnClickListener { launchAutocomplete() }

        binding.locationGpsButton.setOnClickListener { fetchAndFillLocation() }

        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            if (validateInput(title)) {
                publishPost(title, description)
            }
        }
    }

    private fun launchAutocomplete() {
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        ).build(requireContext())
        autocompleteLauncher.launch(intent)
    }

    private fun onPlaceSelected(place: Place) {
        val latlng = place.latLng
        selectedLatLng = latlng
        binding.locationSearchInput.setText(place.name ?: "")
        if (latlng != null) {
            binding.locationCoordsDisplay.text =
                "%.6f, %.6f".format(latlng.latitude, latlng.longitude)
        }
    }

    private fun fetchAndFillLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        setLocationLoading(true)

        val cts = CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(requireActivity())
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (_binding == null) return@addOnSuccessListener
                setLocationLoading(false)
                if (location != null) {
                    selectedLatLng = LatLng(location.latitude, location.longitude)
                    binding.locationCoordsDisplay.text =
                        "%.6f, %.6f".format(location.latitude, location.longitude)
                    binding.locationSearchInput.text?.clear()
                    Toast.makeText(requireContext(), "Location updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(
                        requireView(),
                        "Please enable GPS location services in your device settings",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                setLocationLoading(false)
                Snackbar.make(
                    requireView(),
                    "Failed to get location. Please check GPS settings.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun setLocationLoading(isLoading: Boolean) {
        binding.locationGpsButton.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun validateInput(title: String): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            isValid = false
        } else {
            binding.titleInput.error = null
        }

        if (selectedLatLng == null) {
            Snackbar.make(
                requireView(),
                "Please search for a location or use the GPS button",
                Snackbar.LENGTH_LONG
            ).show()
            isValid = false
        }

        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun publishPost(title: String, description: String) {
        setLoading(true)

        val latlng = selectedLatLng!!

        lifecycleScope.launch {
            val success = PostRepository.shared.createPost(
                title = title,
                description = description,
                imageUri = selectedImageUri,
                latitude = latlng.latitude,
                longitude = latlng.longitude
            )

            if (_binding == null) return@launch
            setLoading(false)
            if (success) {
                Toast.makeText(requireContext(), "Post published successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.mapFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.createPostFragment, inclusive = true)
                        .build()
                )
            } else {
                Toast.makeText(requireContext(), "Failed to publish post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.publishButton.isEnabled = !isLoading
        binding.descriptionInput.isEnabled = !isLoading
        binding.titleInput.isEnabled = !isLoading
        binding.locationSearchInput.isEnabled = !isLoading
        binding.locationGpsButton.isEnabled = !isLoading
        binding.imageUploadContainer.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
