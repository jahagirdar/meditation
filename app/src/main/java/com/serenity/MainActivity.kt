package com.serenity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.ui.navigation.AppNavigation
import com.serenity.ui.theme.SerenityTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepo: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLink = intent?.getStringExtra("deep_link")
        val calmDurationSec = intent?.getIntExtra("calm_duration_sec", 120) ?: 120

        setContent {
            val prefs by prefsRepo.preferences.collectAsState(
                initial = com.serenity.data.preferences.AppPreferences()
            )
            SerenityTheme(themeMode = prefs.themeMode, accentColour = prefs.accentColour) {
                AppNavigation(
                    onboardingComplete = prefs.onboardingComplete,
                    deepLink = deepLink,
                    calmDurationSec = calmDurationSec,
                )
            }
        }
    }
}
