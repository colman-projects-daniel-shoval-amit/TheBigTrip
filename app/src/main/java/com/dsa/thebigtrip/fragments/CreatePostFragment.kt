package com.dsa.thebigtrip.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.repository.posts.PostRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) fetchCurrentLocation()
        else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imagePreview.setImageURI(uri)
            binding.imagePreview.visibility = View.VISIBLE
            binding.imageUploadTextview.visibility = View.GONE
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
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.imagePreview.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.locationInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawable = binding.locationInput.compoundDrawables[2]
                if (drawable != null) {
                    val touchX = event.rawX
                    val fieldRight = binding.locationInput.right
                    val drawableWidth = drawable.bounds.width()
                    val padding = binding.locationInput.paddingEnd
                    if (touchX >= fieldRight - drawableWidth - padding) {
                        fetchCurrentLocation()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val location = binding.locationInput.text.toString().trim()

            if (validateInput(title, description, location)) {
                val parts = location.split(",")
                val latitude = parts[0].trim().toDouble()
                val longitude = parts[1].trim().toDouble()
                publishPost(title, description, selectedImageUri, latitude, longitude)
            }
        }
    }

    private fun validateInput(title: String, description: String, location: String): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            isValid = false
        } else {
            binding.titleInput.error = null
        }

        if (description.isEmpty()) {
            binding.descriptionInput.error = "Description is required"
            isValid = false
        } else {
            binding.descriptionInput.error = null
        }

        if (location.isEmpty()) {
            binding.locationInput.error = "Location is required"
            isValid = false
        } else {
            val parts = location.split(",")
            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                binding.locationInput.error = "Enter valid GPS coordinates (lat,lng)"
                isValid = false
            } else {
                binding.locationInput.error = null
            }
        }

        return isValid
    }

    private fun publishPost(
        title: String,
        description: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
    ) {
        setLoading(true)
        lifecycleScope.launch {
            val success = PostRepository.shared.createPost(
                title = title,
                description = description,
                imageUri = imageUri,
                latitude = latitude,
                longitude = longitude,
            )
            setLoading(false)
            if (success) {
                Toast.makeText(requireContext(), "Post published!", Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.mapFragment)
            } else {
                Toast.makeText(requireContext(), "Failed to publish post", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                binding.locationInput.setText("${location.latitude},${location.longitude}")
            } else {
                Toast.makeText(requireContext(), "Could not get location. Enter manually.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Location fetch failed. Enter manually.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        binding.titleInput.text?.clear()
        binding.descriptionInput.text?.clear()
        binding.locationInput.text?.clear()
        binding.titleInput.error = null
        binding.descriptionInput.error = null
        binding.locationInput.error = null
        selectedImageUri = null
        binding.imagePreview.visibility = View.GONE
        binding.imageUploadTextview.visibility = View.VISIBLE
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.publishButton.isEnabled = !isLoading
        binding.titleInput.isEnabled = !isLoading
        binding.descriptionInput.isEnabled = !isLoading
        binding.locationInput.isEnabled = !isLoading
        binding.imageUploadContainer.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
