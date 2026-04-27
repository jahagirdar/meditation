package com.serenity.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class AppPreferences(
    val themeMode: String = "system",           // system | light | dark | amoled
    val accentColour: String = "slate_blue",
    val showElapsedTime: Boolean = false,       // false = remaining
    val breathingAnimation: Boolean = true,
    val dailyGoalMinutes: Int = 10,
    val onboardingComplete: Boolean = false,
    val lastPresetId: String? = null,
    val reminder1Time: String? = null,          // "HH:mm" or null
    val reminder2Time: String? = null,
    val reminder3Time: String? = null,
    val stressNudgeEnabled: Boolean = true,
    val dimScreenAfterSec: Int = 30,
    // Audio settings
    val useFallbackBell: Boolean = true,
    val useCustomAmbient: Boolean = false,
    val customAmbientUri: String? = null,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE          = stringPreferencesKey("theme_mode")
        val ACCENT_COLOUR       = stringPreferencesKey("accent_colour")
        val SHOW_ELAPSED        = booleanPreferencesKey("show_elapsed")
        val BREATHING_ANIM      = booleanPreferencesKey("breathing_anim")
        val DAILY_GOAL          = intPreferencesKey("daily_goal_minutes")
        val ONBOARDING_DONE     = booleanPreferencesKey("onboarding_complete")
        val LAST_PRESET_ID      = stringPreferencesKey("last_preset_id")
        val REMINDER_1          = stringPreferencesKey("reminder_1")
        val REMINDER_2          = stringPreferencesKey("reminder_2")
        val REMINDER_3          = stringPreferencesKey("reminder_3")
        val STRESS_NUDGE        = booleanPreferencesKey("stress_nudge_enabled")
        val DIM_SCREEN_SEC      = intPreferencesKey("dim_screen_sec")
        val FALLBACK_BELL       = booleanPreferencesKey("fallback_bell")
        val USE_CUSTOM_AMBIENT  = booleanPreferencesKey("use_custom_ambient")
        val CUSTOM_AMBIENT_URI  = stringPreferencesKey("custom_ambient_uri")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppPreferences(
                themeMode         = p[Keys.THEME_MODE] ?: "system",
                accentColour      = p[Keys.ACCENT_COLOUR] ?: "slate_blue",
                showElapsedTime   = p[Keys.SHOW_ELAPSED] ?: false,
                breathingAnimation = p[Keys.BREATHING_ANIM] ?: true,
                dailyGoalMinutes  = p[Keys.DAILY_GOAL] ?: 10,
                onboardingComplete = p[Keys.ONBOARDING_DONE] ?: false,
                lastPresetId      = p[Keys.LAST_PRESET_ID],
                reminder1Time     = p[Keys.REMINDER_1],
                reminder2Time     = p[Keys.REMINDER_2],
                reminder3Time     = p[Keys.REMINDER_3],
                stressNudgeEnabled = p[Keys.STRESS_NUDGE] ?: true,
                dimScreenAfterSec = p[Keys.DIM_SCREEN_SEC] ?: 30,
                useFallbackBell   = p[Keys.FALLBACK_BELL] ?: true,
                useCustomAmbient  = p[Keys.USE_CUSTOM_AMBIENT] ?: false,
                customAmbientUri  = p[Keys.CUSTOM_AMBIENT_URI],
            )
        }

    suspend fun update(transform: suspend (AppPreferences) -> AppPreferences) {
        context.dataStore.edit { prefs ->
            val current = AppPreferences(
                themeMode         = prefs[Keys.THEME_MODE] ?: "system",
                accentColour      = prefs[Keys.ACCENT_COLOUR] ?: "slate_blue",
                showElapsedTime   = prefs[Keys.SHOW_ELAPSED] ?: false,
                breathingAnimation = prefs[Keys.BREATHING_ANIM] ?: true,
                dailyGoalMinutes  = prefs[Keys.DAILY_GOAL] ?: 10,
                onboardingComplete = prefs[Keys.ONBOARDING_DONE] ?: false,
                lastPresetId      = prefs[Keys.LAST_PRESET_ID],
                reminder1Time     = prefs[Keys.REMINDER_1],
                reminder2Time     = prefs[Keys.REMINDER_2],
                reminder3Time     = prefs[Keys.REMINDER_3],
                stressNudgeEnabled = prefs[Keys.STRESS_NUDGE] ?: true,
                dimScreenAfterSec = prefs[Keys.DIM_SCREEN_SEC] ?: 30,
            )
            val updated = transform(current)
            prefs[Keys.THEME_MODE]       = updated.themeMode
            prefs[Keys.ACCENT_COLOUR]    = updated.accentColour
            prefs[Keys.SHOW_ELAPSED]     = updated.showElapsedTime
            prefs[Keys.BREATHING_ANIM]   = updated.breathingAnimation
            prefs[Keys.DAILY_GOAL]       = updated.dailyGoalMinutes
            prefs[Keys.ONBOARDING_DONE]  = updated.onboardingComplete
            updated.lastPresetId?.let { prefs[Keys.LAST_PRESET_ID] = it }
            updated.reminder1Time?.let { prefs[Keys.REMINDER_1] = it }
                ?: prefs.remove(Keys.REMINDER_1)
            updated.reminder2Time?.let { prefs[Keys.REMINDER_2] = it }
                ?: prefs.remove(Keys.REMINDER_2)
            updated.reminder3Time?.let { prefs[Keys.REMINDER_3] = it }
                ?: prefs.remove(Keys.REMINDER_3)
            prefs[Keys.STRESS_NUDGE]     = updated.stressNudgeEnabled
            prefs[Keys.DIM_SCREEN_SEC]   = updated.dimScreenAfterSec
            prefs[Keys.FALLBACK_BELL]    = updated.useFallbackBell
            prefs[Keys.USE_CUSTOM_AMBIENT] = updated.useCustomAmbient
            updated.customAmbientUri?.let { prefs[Keys.CUSTOM_AMBIENT_URI] = it }
                ?: prefs.remove(Keys.CUSTOM_AMBIENT_URI)
        }
    }
}
