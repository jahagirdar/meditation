package com.serenity.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.SoundPool
import android.net.Uri
import com.serenity.domain.model.AmbientSound
import com.serenity.domain.model.BellSound
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralised audio loader used by both MeditationTimerService and PranayamaViewModel.
 *
 * Bell sounds:
 *   - Tries to load the raw asset (e.g. res/raw/bell_tibetan_bowl.mp3)
 *   - If the asset is missing (resId == 0) and [useFallback] is true,
 *     falls back to the system default notification sound via RingtoneManager
 *
 * Ambient sounds:
 *   - Tries raw asset first
 *   - Falls back to a [customAmbientUri] (Uri from MediaStore / file picker) if supplied
 *   - Returns null if neither is available
 */
@Singleton
class SerenityAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // ── Bell / SoundPool ──────────────────────────────────────────────────

    /**
     * Load a bell sound into the given SoundPool.
     * Returns the SoundPool sound ID (> 0) on success, or 0 if unavailable.
     *
     * If the raw resource is missing and [fallbackToNotification] is true,
     * the system default notification URI is played directly (outside SoundPool)
     * via [playFallbackBell].
     */
    fun loadBell(
        pool: SoundPool,
        rawName: String,
    ): Int {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        return if (resId != 0) pool.load(context, resId, 1) else 0
    }

    /**
     * Play the system notification sound as a one-shot bell fallback.
     * Called when a bell sound ID is 0 (raw asset missing).
     */
    fun playFallbackBell(volume: Float = 1.0f) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                setVolume(volume, volume)
            }
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            // Nothing to do — silent fallback
        }
    }

    // ── Ambient / MediaPlayer ────────────────────────────────────────────

    /**
     * Create a looping MediaPlayer for ambient audio.
     *
     * Priority:
     *   1. Raw resource (e.g. res/raw/ambient_rain.mp3)
     *   2. [customUri] — a content:// or file:// URI from the device's media library
     *
     * Returns null if neither source is available.
     */
    fun createAmbientPlayer(
        sound: AmbientSound,
        customUri: Uri? = null,
    ): MediaPlayer? {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        // 1. Try built-in raw asset
        if (sound != AmbientSound.NONE && sound.rawRes.isNotBlank()) {
            val resId = context.resources.getIdentifier(sound.rawRes, "raw", context.packageName)
            if (resId != 0) {
                return MediaPlayer.create(context, resId)?.also { it.isLooping = true }
            }
        }

        // 2. Try custom URI from file picker
        if (customUri != null) {
            return try {
                MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(context, customUri)
                    isLooping = true
                    prepare()
                }
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    /**
     * Load a pranayama phase cue, with system notification sound as fallback.
     */
    fun loadPranayamaCue(pool: SoundPool, rawName: String): Int =
        loadBell(pool, rawName)   // same mechanism — 0 means "use fallbackBell"

    // ── Utility ──────────────────────────────────────────────────────────

    fun isBellAvailable(rawName: String): Boolean =
        context.resources.getIdentifier(rawName, "raw", context.packageName) != 0

    fun missingBells(): List<BellSound> =
        BellSound.entries.filter { it != BellSound.SILENT && !isBellAvailable(it.rawRes) }

    fun missingAmbients(): List<AmbientSound> =
        AmbientSound.entries.filter {
            it != AmbientSound.NONE &&
            context.resources.getIdentifier(it.rawRes, "raw", context.packageName) == 0
        }

    fun missingPranayamaCues(): List<String> =
        listOf("prana_inhale", "prana_hold", "prana_exhale",
               "prana_round_complete", "prana_om", "prana_session_end")
            .filter { context.resources.getIdentifier(it, "raw", context.packageName) == 0 }
}
