package com.dsa.thebigtrip

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dsa.thebigtrip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // targetSdk=36 → Android 15 enforces edge-to-edge. Explicitly opt in and handle insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isNightMode

        // Toolbar: extend background behind status bar; push content below it via padding.
        val toolbarOriginalTop = binding.toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, toolbarOriginalTop + statusBar.top, view.paddingRight, view.paddingBottom)
            insets
        }

        // BottomNav: stay above gesture/button navigation bar.
        val bottomNavOriginalBottom = binding.bottomNav.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomNavOriginalBottom + navBar.bottom)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main)
                    as NavHostFragment

        val navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mapFragment,
                R.id.createPostFragment,
                R.id.profileFragment,
                R.id.myPostsFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // NavigationUI's listener (registered above) overwrites the title with the
            // destination label. Reset it here so the banner always reads "The Big Trip".
            supportActionBar?.title = getString(R.string.app_name)

            binding.bottomNav.visibility = if (destination.id == R.id.postDetailsFragment) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        return NavigationUI.navigateUp(navHostFragment.navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
