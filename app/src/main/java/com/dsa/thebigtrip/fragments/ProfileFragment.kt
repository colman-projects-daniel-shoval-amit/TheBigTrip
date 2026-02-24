package com.dsa.thebigtrip.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dsa.thebigtrip.Auth.AuthActivity
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.databinding.FragmentProfileBinding
import com.dsa.thebigtrip.utils.ImageUtil
import com.dsa.thebigtrip.utils.bitmap
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditMode = false
    private var imageSelected = false
    private var currentUser: User? = null

    private lateinit var auth: FirebaseAuth

    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                binding.ivProfileImage.setImageURI(uri)
                imageSelected = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            val user = UserRepository.Companion.shared.getUserById(uid)
            currentUser = user

            if (user != null) {
                binding.tvWelcome.text = "Welcome, ${user.fullName ?: "User"}!"
                binding.tvUserName.text = user.fullName ?: "Not set"
                binding.tvUserEmail.text = user.email ?: "Not set"

                // Load profile image
                ImageUtil.loadCircleImage(
                    binding.ivProfileImage,
                    user.imageUri,
                    R.drawable.ic_person
                )
            }
        }
    }

    private fun setupListeners() {
        binding.fabPickImage.setOnClickListener {
            if (isEditMode) {
                openPhotoPicker()
            }
        }

        binding.cardProfileImage.setOnClickListener {
            if (isEditMode) {
                openPhotoPicker()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            if (isEditMode) {
                saveProfile()
            } else {
                enterEditMode()
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun openPhotoPicker() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.tilEditName.visibility = View.VISIBLE
        binding.etEditName.setText(currentUser?.fullName ?: "")
        binding.btnEditProfile.text = "Save Changes"
        binding.btnEditProfile.setBackgroundColor(0xFF2E7D32.toInt())
        binding.fabPickImage.visibility = View.VISIBLE
    }

    private fun exitEditMode() {
        isEditMode = false
        imageSelected = false
        binding.tilEditName.visibility = View.GONE
        binding.fabPickImage.visibility = View.GONE
        binding.btnEditProfile.text = "Edit Profile"
    }

    private fun saveProfile() {
        val name = binding.etEditName.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilEditName.error = "Name is required"
            return
        }

        val uid = auth.currentUser?.uid ?: return
        setLoading(true)

        lifecycleScope.launch {
            try {
                var imageUrl = currentUser?.imageUri

                // Upload new image if selected
                if (imageSelected) {
                    val bitmap = binding.ivProfileImage.bitmap
                    if (bitmap != null) {
                        val uploadedUrl = ImageUtil.uploadUserProfileImage(bitmap, uid)
                        if (uploadedUrl != null) {
                            imageUrl = uploadedUrl
                        }
                    }
                }

                val updatedUser = User(
                    uid = uid,
                    fullName = name,
                    email = currentUser?.email ?: auth.currentUser?.email,
                    imageUri = imageUrl,
                )

                UserRepository.Companion.shared.updateUser(updatedUser)
                currentUser = updatedUser

                // Update UI
                binding.tvWelcome.text = "Welcome, $name!"
                binding.tvUserName.text = name

                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                exitEditMode()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnEditProfile.isEnabled = !isLoading
        binding.btnLogout.isEnabled = !isLoading
        binding.fabPickImage.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}