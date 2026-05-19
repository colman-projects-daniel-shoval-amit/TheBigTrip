package com.dsa.thebigtrip.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dsa.thebigtrip.Auth.AuthActivity
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.repository.users.UserRepository
import com.dsa.thebigtrip.databinding.FragmentProfileBinding
import com.dsa.thebigtrip.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditMode = false
    private var selectedImageUri: Uri? = null
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
                selectedImageUri = uri
                binding.ivProfileImage.setImageURI(uri)
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
            val user = UserRepository.shared.getUserById(uid)
            currentUser = user

            user?.let {
                binding.tvWelcome.text = getString(R.string.welcome_message, it.fullName ?: "User")
                binding.tvUserName.text = it.fullName ?: "Not set"
                binding.tvUserEmail.text = it.email ?: "Not set"

                ImageUtil.loadCircleImage(
                    binding.ivProfileImage,
                    it.imageUri,
                    R.drawable.ic_person
                )
            }
        }
    }

    private fun setupListeners() {
        binding.fabPickImage.setOnClickListener { if (isEditMode) openPhotoPicker() }
        binding.cardProfileImage.setOnClickListener { if (isEditMode) openPhotoPicker() }

        binding.btnEditProfile.setOnClickListener {
            if (isEditMode) saveProfile() else enterEditMode()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
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
        binding.btnEditProfile.text = getString(R.string.save_changes)
        binding.btnEditProfile.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
        binding.fabPickImage.visibility = View.VISIBLE
    }

    private fun exitEditMode() {
        isEditMode = false
        selectedImageUri = null
        binding.tilEditName.visibility = View.GONE
        binding.fabPickImage.visibility = View.GONE
        binding.btnEditProfile.text = getString(R.string.edit_profile)
        // Reset to default theme color if necessary, or just keep it green
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

                selectedImageUri?.let { uri ->
                    val uploadedUrl = UserRepository.shared.uploadProfilePicture(uri, uid)
                    if (uploadedUrl != null) {
                        imageUrl = uploadedUrl
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                val updatedUser = User(
                    uid = uid,
                    fullName = name,
                    email = currentUser?.email ?: auth.currentUser?.email,
                    imageUri = imageUrl,
                )

                UserRepository.shared.updateUser(updatedUser)
                currentUser = updatedUser

                binding.tvWelcome.text = getString(R.string.welcome_message, name)
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
