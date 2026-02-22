package com.dsa.thebigtrip

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dsa.thebigtrip.Auth.AuthActivity
import com.dsa.thebigtrip.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.tvUserVerified.text = if (user.isEmailVerified) "Yes ✓" else "No ✗"
            binding.tvUserVerified.setTextColor(
                if (user.isEmailVerified) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt()
            )
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}