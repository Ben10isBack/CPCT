package com.example.cpcontesttracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpcontesttracker.ui.CPCTTheme
import com.example.cpcontesttracker.ui.ContestListScreen
import com.example.cpcontesttracker.ui.ContestViewModel
import com.example.cpcontesttracker.ui.PlatformSelectionScreen
import com.example.cpcontesttracker.ui.SplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark system bars to match the app (light icons)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Setup SharedPreferences to remember user choices
        val sharedPref = getSharedPreferences("cp_prefs", Context.MODE_PRIVATE)

        setContent {
            CPCTTheme {
                val viewModel: ContestViewModel = viewModel()

                // Startup screen only on a fresh launch, not after rotation
                var showSplash by rememberSaveable { mutableStateOf(true) }

                // State to control which screen is showing
                var hasSelectedPlatforms by remember {
                    mutableStateOf(sharedPref.getBoolean("has_selected", false))
                }
                var selectedPlatforms by remember {
                    mutableStateOf(sharedPref.getStringSet("platforms", setOf()) ?: setOf())
                }

                // Start loading right away (even behind the startup screen).
                // The ViewModel skips the fetch if these platforms are already loaded (e.g. rotation).
                LaunchedEffect(hasSelectedPlatforms, selectedPlatforms) {
                    if (hasSelectedPlatforms) viewModel.fetchContests(selectedPlatforms)
                }

                Crossfade(targetState = showSplash, label = "splash") { splash ->
                    when {
                        splash -> SplashScreen(onFinished = { showSplash = false })

                        // Show Onboarding / Platform Selection Screen
                        !hasSelectedPlatforms -> PlatformSelectionScreen(
                            initialSelection = selectedPlatforms,
                            onSave = { platforms ->
                                sharedPref.edit()
                                    .putStringSet("platforms", platforms)
                                    .putBoolean("has_selected", true)
                                    .apply()

                                selectedPlatforms = platforms
                                hasSelectedPlatforms = true
                            }
                        )

                        else -> ContestListScreen(
                            viewModel = viewModel,
                            onFilterClick = { hasSelectedPlatforms = false }
                        )
                    }
                }
            }
        }
    }
}
