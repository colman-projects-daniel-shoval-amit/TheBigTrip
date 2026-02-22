package com.dsa.thebigtrip.Auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dsa.thebigtrip.MainActivity
import com.dsa.thebigtrip.R
import  com.dsa.thebigtrip.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        Log.d("TAG", "HERE ")

        auth = FirebaseAuth.getInstance()
        //for Testing add this shit if not we need to register the app
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        // If user is already signed in, navigate to main activity
        val user = auth.currentUser

        if (user != null) {
            navigateToMain()
            Log.d("TAG", "user $user already logged in ")
        }
    }

    fun navigateToMain() {
         val intent = Intent(this, MainActivity::class.java)
         startActivity(intent)
         finish()
    }
}
