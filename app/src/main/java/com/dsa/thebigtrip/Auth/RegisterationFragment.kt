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
        Log.d("TAG", "createUserWithEmail:success name $name email $email pass $password")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            setLoading(false)
                            if (profileTask.isSuccessful) {
                                user.sendEmailVerification().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Snackbar.make(binding.root, "Account created! Verification email sent.", Snackbar.LENGTH_SHORT)
                                            .setBackgroundTint("#4CAF50".toColorInt())
                                            .setTextColor(Color.WHITE)
                                            .setActionTextColor("#FFEB3B".toColorInt())
                                            .show()
                                    } else {
                                        Snackbar.make(binding.root, "Account created! Verification email failed to sent.", Snackbar.LENGTH_SHORT)
                                            .setBackgroundTint("#ff4545".toColorInt())
                                            .setTextColor(Color.WHITE)
                                            .setActionTextColor("#FFEB3B".toColorInt())
                                            .show()
                                        Log.e("AUTH", "Send failed: ${task.exception?.message}")
                                    }
                                }


                                (activity as? AuthActivity)?.navigateToMain()
                            }
                        }
                } else {
                    setLoading(false)
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already") == true ->
                            "An account with this email already exists"
                        task.exception?.message?.contains("email address is badly") == true ->
                            "Invalid email format"
                        task.exception?.message?.contains("weak password") == true ->
                            "Password is too weak"
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
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