package com.dsa.thebigtrip.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    private val GPS_PATTERN: Pattern = Pattern.compile(
        "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$"
    )

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

        // The GPS icon is a drawableEnd on the EditText — detect taps in that region.
        binding.locationInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val gpsIcon = binding.locationInput.compoundDrawablesRelative[2]
                    ?: binding.locationInput.compoundDrawables[2]
                if (gpsIcon != null) {
                    val iconStartX = binding.locationInput.width -
                        binding.locationInput.paddingEnd -
                        gpsIcon.intrinsicWidth.coerceAtLeast(1)
                    if (event.x >= iconStartX) {
                        fetchAndFillLocation()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val location = binding.locationInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()

            if (validateInput(title, location)) {
                publishPost(title, description, location)
            }
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
                    binding.locationInput.setText("${location.latitude}, ${location.longitude}")
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
        binding.locationInput.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun validateInput(title: String, location: String): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            isValid = false
        } else {
            binding.titleInput.error = null
        }

        if (location.isEmpty()) {
            binding.locationInput.error = "Location is required"
            isValid = false
        } else if (!GPS_PATTERN.matcher(location).matches()) {
            binding.locationInput.error = "Enter valid GPS coordinates (lat, long)"
            isValid = false
        } else {
            binding.locationInput.error = null
        }

        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun publishPost(title: String, description: String, location: String) {
        setLoading(true)

        val coords = location.split(",").map { it.trim().toDouble() }
        val latitude = coords[0]
        val longitude = coords[1]

        lifecycleScope.launch {
            val success = PostRepository.shared.createPost(
                title = title,
                description = description,
                imageUri = selectedImageUri,
                latitude = latitude,
                longitude = longitude
            )

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
        binding.locationInput.isEnabled = !isLoading
        binding.imageUploadContainer.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
