package com.example.cpcontesttracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpcontesttracker.ui.ContestListScreen
import com.example.cpcontesttracker.ui.ContestViewModel
import com.example.cpcontesttracker.ui.PlatformSelectionScreen

class MainActivity : ComponentActivity() {

    // TODO: Replace with your actual Clist username and API key!
    // Format MUST be exactly: "ApiKey your_username:your_api_key"
    private val API_KEY = "ApiKey the_forever_knight:1278e5a88dfcfc022f538808c04f0a48e296db84"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup SharedPreferences to remember user choices
        val sharedPref = getSharedPreferences("cp_prefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ContestViewModel = viewModel()
                    
                    // State to control which screen is showing
                    var hasSelectedPlatforms by remember { 
                        mutableStateOf(sharedPref.getBoolean("has_selected", false)) 
                    }
                    var selectedPlatforms by remember { 
                        mutableStateOf(sharedPref.getStringSet("platforms", setOf()) ?: setOf()) 
                    }

                    if (!hasSelectedPlatforms) {
                        // Show Onboarding / Platform Selection Screen
                        PlatformSelectionScreen(
                            initialSelection = selectedPlatforms,
                            onSave = { platforms ->
                                // Save to SharedPreferences
                                sharedPref.edit()
                                    .putStringSet("platforms", platforms)
                                    .putBoolean("has_selected", true)
                                    .apply()
                                
                                selectedPlatforms = platforms
                                hasSelectedPlatforms = true
                            }
                        )
                    } else {
                        // Show Contest List Screen
                        LaunchedEffect(selectedPlatforms) {
                            viewModel.fetchContests(selectedPlatforms)
                        }

                        ContestListScreen(
                            viewModel = viewModel,
                            onFilterClick = {
                                // Go back to platform selection
                                hasSelectedPlatforms = false
                            }
                        )
                    }
                }
            }
        }
    }
}
