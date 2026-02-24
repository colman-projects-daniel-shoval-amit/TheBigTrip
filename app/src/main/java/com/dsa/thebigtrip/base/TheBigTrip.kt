package com.dsa.thebigtrip.base

import android.app.Application
import android.content.Context

class TheBigTrip: Application() {
    companion object Globals {
        var appContext: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}