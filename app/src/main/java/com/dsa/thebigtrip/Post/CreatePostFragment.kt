package com.dsa.thebigtrip.Post

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val repository = PostRepository.shared

    private var selectedImageUri: Uri? = null

    private val GPS_PATTERN: Pattern = Pattern.compile(
        "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$"
    )

    // 📸 Image picker with preview
    private val imagePickerLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                selectedImageUri = it

                // Hide text
                binding.imageUploadTextview.visibility = View.GONE

                // Create ImageView preview
                val imageView = ImageView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(it)
                }

                // Replace content
                binding.imageUploadContainer.removeAllViews()
                binding.imageUploadContainer.addView(imageView)
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
        setupListeners()
    }

    private fun setupListeners() {

        // 📸 Open gallery
        binding.imageUploadContainer.setOnClickListener {
            openGallery()
        }

        // 🚀 Publish
        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val location = binding.locationInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()

            if (validateInput(title, location)) {
                publishPost(title, description, location)
            }
        }
    }

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
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
            binding.locationInput.error = "Use format: lat,lng"
            isValid = false
        } else {
            binding.locationInput.error = null
        }

        return isValid
    }

    private fun publishPost(
        title: String,
        description: String,
        location: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val (lat, lng) = parseLocation(location)

                val post = Post(
                    id = System.currentTimeMillis().toString(),
                    userId = "user1",
                    caption = title,
                    imageUrl = selectedImageUri?.toString(),
                    createdAt = System.currentTimeMillis(),
                    locationName = description,
                    latitude = lat,
                    longitude = lng
                )

                repository.addPost(post)

                Snackbar.make(binding.root, "Post published!", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint("#4CAF50".toColorInt())
                    .setTextColor(Color.WHITE)
                    .show()

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

    private fun parseLocation(location: String): Pair<Double, Double> {
        val parts = location.split(",")
        return Pair(parts[0].trim().toDouble(), parts[1].trim().toDouble())
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.publishButton.isEnabled = !isLoading
        binding.descriptionInput.isEnabled = !isLoading
        binding.titleInput.isEnabled = !isLoading
        binding.locationInput.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}