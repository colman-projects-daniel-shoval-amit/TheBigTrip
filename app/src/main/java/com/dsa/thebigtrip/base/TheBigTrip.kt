package com.dsa.thebigtrip.base

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.firebase.FirebaseApp

class TheBigTrip: Application(), OnMapsSdkInitializedCallback {
    companion object Globals {
        var appContext: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Maps SDK with the latest renderer to avoid potential database lock issues
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)

        // Note: App Check is disabled for now as it needs console configuration (returning 403)
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d("MapsInitializer", "The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> Log.d("MapsInitializer", "The legacy version of the renderer is used.")
        }
    }
}