package com.serenity.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.service.SerenityAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioSettingsState(
    val missingBells: List<String>          = emptyList(),
    val missingPranayamaCues: List<String>  = emptyList(),
    val missingAmbients: List<String>       = emptyList(),
    val useFallbackForMissing: Boolean      = true,
    val useCustomAmbient: Boolean           = false,
    val customAmbientUri: Uri?              = null,
    val customAmbientName: String?          = null,
    val permissionJustGranted: Boolean      = false,
)

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioManager: SerenityAudioManager,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioSettingsState())
    val state: StateFlow<AudioSettingsState> = _state.asStateFlow()

    init {
        // Scan for missing files
        _state.update { s ->
            s.copy(
                missingBells       = audioManager.missingBells().map { it.rawRes },
                missingPranayamaCues = audioManager.missingPranayamaCues(),
                missingAmbients    = audioManager.missingAmbients().map { it.rawRes },
            )
        }
        // Load persisted preferences
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _state.update { s ->
                    s.copy(
                        useFallbackForMissing = prefs.useFallbackBell,
                        useCustomAmbient      = prefs.useCustomAmbient,
                        customAmbientUri      = prefs.customAmbientUri?.let { Uri.parse(it) },
                        customAmbientName     = prefs.customAmbientUri?.let { uri ->
                            resolveDisplayName(Uri.parse(uri))
                        },
                    )
                }
            }
        }
    }

    fun toggleFallback() {
        viewModelScope.launch {
            prefsRepo.update { it.copy(useFallbackBell = !it.useFallbackBell) }
        }
    }

    fun setUseCustomAmbient(use: Boolean) {
        _state.update { it.copy(useCustomAmbient = use) }
        viewModelScope.launch {
            prefsRepo.update { it.copy(useCustomAmbient = use) }
        }
    }

    fun setCustomAmbientUri(uri: Uri) {
        // Persist a durable permission so the URI survives app restarts
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) { /* file:// URIs don't support this */ }

        val name = resolveDisplayName(uri)
        _state.update { it.copy(customAmbientUri = uri, customAmbientName = name) }
        viewModelScope.launch {
            prefsRepo.update { it.copy(customAmbientUri = uri.toString()) }
        }
    }

    fun clearCustomAmbient() {
        _state.update { it.copy(customAmbientUri = null, customAmbientName = null) }
        viewModelScope.launch {
            prefsRepo.update { it.copy(customAmbientUri = null) }
        }
    }

    fun onAudioPermissionGranted() {
        _state.update { it.copy(permissionJustGranted = true) }
    }

    fun consumePermissionGranted() {
        _state.update { it.copy(permissionJustGranted = false) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null,
            )
            cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
