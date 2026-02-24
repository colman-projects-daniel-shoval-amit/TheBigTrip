package com.dsa.thebigtrip.Auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.MainActivity
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.etEmail.setOnFocusChangeListener { _, _ -> binding.tilEmail.error = null }
        binding.etPassword.setOnFocusChangeListener { _, _ -> binding.tilPassword.error = null }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

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
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun loginUser(email: String, password: String) {
        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Snackbar.make(binding.root, "Login successful!", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint("#4CAF50".toColorInt())
                        .setTextColor(Color.WHITE)
                        .setActionTextColor("#FFEB3B".toColorInt())
                        .show()


                    (activity as? AuthActivity)?.navigateToMain()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email"
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Incorrect password"
                        task.exception?.message?.contains("blocked") == true ->
                            "Account temporarily blocked. Try again later."
                        else -> task.exception?.message ?: "Login failed"
                    }
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint("#ff4545".toColorInt())
                        .setTextColor(Color.WHITE)
                        .setActionTextColor("#FFEB3B".toColorInt())
                        .show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}