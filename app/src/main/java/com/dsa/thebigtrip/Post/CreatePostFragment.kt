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
import com.bumptech.glide.Glide
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.dsa.thebigtrip.utils.ImageUtil
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val repository = PostRepository.shared

    private var selectedImageUri: Uri? = null
    private var currentPost: Post? = null

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
                showImagePreview(it.toString())
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
        loadPostForEditIfNeeded()
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
                savePost(title, description, location)
            }
        }
    }

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
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
                    binding.locationInput.setText("${post.latitude},${post.longitude}")
                }

                if (!post.imageUrl.isNullOrEmpty()) {
                    showImagePreview(post.imageUrl)
                }
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

    private fun savePost(
        title: String,
        description: String,
        location: String
    ) {
        if (currentPost == null) {
            publishPost(title, description, location)
        } else {
            updatePost(title, description, location)
        }
    }

    private fun publishPost(
        title: String,
        description: String,
        location: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("Please log in before publishing")
                val (lat, lng) = parseLocation(location)
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
                    locationName = description,
                    latitude = lat,
                    longitude = lng
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
        description: String,
        location: String
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

                val (lat, lng) = parseLocation(location)
                val imageUrl = selectedImageUri?.let {
                    ImageUtil.uploadPostImage(requireContext(), it, existingPost.id)
                        ?: throw IllegalStateException("Failed to upload image")
                } ?: existingPost.imageUrl

                val updatedPost = existingPost.copy(
                    caption = title,
                    imageUrl = imageUrl,
                    locationName = description,
                    latitude = lat,
                    longitude = lng
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

    private fun parseLocation(location: String): Pair<Double, Double> {
        val parts = location.split(",")
        return Pair(parts[0].trim().toDouble(), parts[1].trim().toDouble())
    }

    private fun setLoading(isLoading: Boolean) {
        val currentBinding = _binding ?: return

        currentBinding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        currentBinding.publishButton.isEnabled = !isLoading
        currentBinding.descriptionInput.isEnabled = !isLoading
        currentBinding.titleInput.isEnabled = !isLoading
        currentBinding.locationInput.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
