package com.serenity.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.domain.model.*
import com.serenity.ui.assessment.AssessmentScreen
import com.serenity.ui.history.HistoryScreen
import com.serenity.ui.home.HomeScreen
import com.serenity.ui.onboarding.OnboardingScreen
import com.serenity.ui.pranayama.PranayamaCompleteScreen
import com.serenity.ui.pranayama.PranayamaPickerScreen
import com.serenity.ui.pranayama.PranayamaSessionScreen
import com.serenity.ui.pranayama.PranayamaViewModel
import com.serenity.ui.session.SessionCompleteSheet
import com.serenity.ui.session.SessionScreen
import com.serenity.crash.CrashHandler
import com.serenity.crash.CrashReportScreen
import com.serenity.ui.settings.AudioSettingsScreen
import com.serenity.ui.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Screen(val route: String) {
    object Onboarding          : Screen("onboarding")
    object Home                : Screen("home")
    object Session             : Screen("session")
    object History             : Screen("history")
    object Settings            : Screen("settings")
    object Assessment          : Screen("assessment")
    object PranayamaPicker     : Screen("pranayama_picker")
    object PranayamaSession    : Screen("pranayama_session")
    object PranayamaComplete   : Screen("pranayama_complete")
    object AudioSettings       : Screen("audio_settings")
    object CrashReport         : Screen("crash_report")
}

@HiltViewModel
class NavViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    fun markOnboardingComplete() {
        viewModelScope.launch { prefsRepo.update { it.copy(onboardingComplete = true) } }
    }
}

@Composable
fun AppNavigation(
    onboardingComplete: Boolean,
    deepLink: String?,
    calmDurationSec: Int,
) {
    val navController               = rememberNavController()
    val navViewModel: NavViewModel  = hiltViewModel()

    var pendingPreset               by remember { mutableStateOf<Preset?>(null) }
    var completedSec                by remember { mutableStateOf<Int?>(null) }
    var showCompletionSheet         by remember { mutableStateOf(false) }
    var completedPranayamaSession   by remember { mutableStateOf<com.serenity.domain.model.PranayamaSession?>(null) }

    val startDest = if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route

    LaunchedEffect(deepLink) {
        if (deepLink == "calm_start") {
            pendingPreset = Preset(
                name         = "Quick Calm",
                durationSec  = calmDurationSec,
                startBell    = BellSound.SOFT_CHIME,
                endBell      = BellSound.SOFT_CHIME,
                ambientSound = AmbientSound.RAIN,
            )
            navController.navigate(Screen.Session.route)
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navViewModel.markOnboardingComplete()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onStartSession       = { preset ->
                    pendingPreset = preset
                    navController.navigate(Screen.Session.route)
                },
                onNavigateHistory    = { navController.navigate(Screen.History.route) },
                onNavigateSettings   = { navController.navigate(Screen.Settings.route) },
                onNavigateAssessment = { navController.navigate(Screen.Assessment.route) },
                onNavigatePranayama  = { navController.navigate(Screen.PranayamaPicker.route) },
            )
        }

        composable(Screen.Session.route) {
            val preset = pendingPreset
            if (preset != null) {
                SessionScreen(
                    preset     = preset,
                    onComplete = { actual -> completedSec = actual; showCompletionSheet = true },
                    onExit     = { navController.popBackStack() },
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack                = { navController.popBackStack() },
                onNavigateAudio       = { navController.navigate(Screen.AudioSettings.route) },
                onNavigateCrashReport = { navController.navigate(Screen.CrashReport.route) },
            )
        }

        composable(Screen.Assessment.route) {
            AssessmentScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AudioSettings.route) {
            AudioSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CrashReport.route) {
            CrashReportScreen(onBack = { navController.popBackStack() })
        }

        // ── Pranayama — nested graph so picker+session share one ViewModel ──
        navigation(
            startDestination = Screen.PranayamaPicker.route,
            route            = "pranayama_graph",
        ) {
            composable(Screen.PranayamaPicker.route) { backStackEntry ->
                // Scope ViewModel to the nested graph — same instance for all pranayama screens
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("pranayama_graph")
                }
                val pranayamaViewModel: PranayamaViewModel = hiltViewModel(graphEntry)

                PranayamaPickerScreen(
                    onBack         = { navController.popBackStack() },
                    onStartSession = { technique, rounds ->
                        // startSession on the shared VM BEFORE navigating
                        // so sessionState is non-null when SessionScreen composes
                        pranayamaViewModel.startSession(technique, rounds)
                        navController.navigate(Screen.PranayamaSession.route)
                    },
                    viewModel = pranayamaViewModel,
                )
            }

            composable(Screen.PranayamaSession.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("pranayama_graph")
                }
                val pranayamaViewModel: PranayamaViewModel = hiltViewModel(graphEntry)

                PranayamaSessionScreen(
                    onComplete = { session ->
                        completedPranayamaSession = session
                        navController.navigate(Screen.PranayamaComplete.route) {
                            popUpTo(Screen.PranayamaSession.route) { inclusive = true }
                        }
                    },
                    onExit    = { navController.popBackStack() },
                    viewModel = pranayamaViewModel,
                )
            }
        }

        composable(Screen.PranayamaComplete.route) {
            val session = completedPranayamaSession
            if (session != null) {
                PranayamaCompleteScreen(
                    session = session,
                    onDone  = {
                        completedPranayamaSession = null
                        navController.popBackStack(Screen.Home.route, false)
                    },
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack(Screen.Home.route, false) }
            }
        }
    }

    if (showCompletionSheet && completedSec != null) {
        SessionCompleteSheet(
            actualSec = completedSec!!,
            streak    = 0,
            onDone    = { _ -> showCompletionSheet = false; navController.popBackStack(Screen.Home.route, false) },
        )
    }
}
