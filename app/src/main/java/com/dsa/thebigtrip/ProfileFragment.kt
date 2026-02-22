package com.dsa.thebigtrip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dsa.thebigtrip.Auth.AuthActivity
import com.dsa.thebigtrip.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        displayUserInfo()
        setupListeners()
    }

    private fun displayUserInfo() {
        val user = auth.currentUser

        if (user != null) {
            binding.tvWelcome.text = "Welcome, ${user.displayName ?: "User"}!"
            binding.tvUserName.text = user.displayName ?: "Not set"
            binding.tvUserEmail.text = user.email ?: "Not set"
            binding.tvUserUid.text = user.uid
            binding.tvUserVerified.text =
                if (user.isEmailVerified) "Yes ✓" else "No ✗"

            binding.tvUserVerified.setTextColor(
                if (user.isEmailVerified)
                    0xFF2E7D32.toInt()
                else
                    0xFFD32F2F.toInt()
            )
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()

            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}