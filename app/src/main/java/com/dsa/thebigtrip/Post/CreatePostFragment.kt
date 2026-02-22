package com.dsa.thebigtrip.Post

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dsa.thebigtrip.databinding.FragmentCreatePostBinding
import com.google.android.material.snackbar.Snackbar
import java.util.regex.Pattern

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null

    private val binding get() = _binding!!

    private val GPS_PATTERN: Pattern = Pattern.compile(
        "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
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
            binding.locationInput.error = "Enter a valid email"
            isValid = false
        } else {
            binding.locationInput.error = null
        }

        return isValid
    }

    private fun setupListeners() {
        binding.publishButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val location = binding.locationInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val image = binding.imageUploadTextview.text.toString().trim()

            if (validateInput(title, location)) {
                publishPost(title,description,image, location)
            }
        }
    }

    private fun publishPost(title: String, description: String, image: String, location: String) {
        setLoading(true)
//        post.submitPost(title, description, image, location)
//            .addOnCompleteListener(requireActivity()) { task ->
//                setLoading(false)
//                if (task.isSuccessful) {
//                    Snackbar.make(binding.root, "Publish successful!", Snackbar.LENGTH_SHORT)
//                        .setBackgroundTint("#4CAF50".toColorInt())
//                        .setTextColor(Color.WHITE)
//                        .setActionTextColor("#FFEB3B".toColorInt())
//                        .show()
//                    (activity as? AuthActivity)?.navigateToMain()
//                } else {
//                    val errorMessage = when {
//                        task.exception?.message?.contains("Title found") == true ->
//                            "Post with this Title already exists"
//                        task.exception?.message?.contains("location is invalid") == true ->
//                            "Incorrect location"
//                        task.exception?.message?.contains("blocked") == true ->
//                            "Account temporarily blocked. Try again later."
//                        else -> task.exception?.message ?: "Login failed"
//                    }
//                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT)
//                        .setBackgroundTint("#ff4545".toColorInt())
//                        .setTextColor(Color.WHITE)
//                        .setActionTextColor("#FFEB3B".toColorInt())
//                        .show()
//                }
//            }
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