package com.dsa.thebigtrip.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.data.repository.posts.PostRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
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

        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val location = binding.locationInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()

            if (validateInput(title, location)) {
                publishPost(title, description, location)
            }
        }
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
                findNavController().popBackStack()
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
