package com.dsa.thebigtrip.Auth

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                registerUser(name, email, password)
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        // Clear errors on focus
        binding.etName.setOnFocusChangeListener { _, _ -> binding.tilName.error = null }
        binding.etEmail.setOnFocusChangeListener { _, _ -> binding.tilEmail.error = null }
        binding.etPassword.setOnFocusChangeListener { _, _ -> binding.tilPassword.error = null }
        binding.etConfirmPassword.setOnFocusChangeListener { _, _ -> binding.tilConfirmPassword.error = null }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else if (!password.any { it.isUpperCase() }) {
            binding.tilPassword.error = "Password must contain an uppercase letter"
            isValid = false
        } else if (!password.any { it.isDigit() }) {
            binding.tilPassword.error = "Password must contain a number"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }
    private fun registerUser(name: String, email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
                val uid = auth.currentUser?.uid ?: return@launch // the @ in the return refers to what scope we are returning so here we want to return for the coroutine and not the whole register user - for future me
                val user = User(
                    uid = uid,
                    fullName = name,
                    email = email,
                    imageUri = "bob"
                )

                Log.d("TAG", "User created with UID: $uid")
                UserRepository.shared.addUser(user)

                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_LONG).show()
                (activity as? AuthActivity)?.navigateToMain()

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("email address is already") == true ->
                        "An account with this email already exists"
                    e.message?.contains("email address is badly") == true ->
                        "Invalid email format"
                    e.message?.contains("weak password") == true ->
                        "Password is too weak"
                    else -> e.message ?: "Registration failed"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}