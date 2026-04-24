package com.serenity.watch

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.serenity.MainActivity
import com.serenity.R
import com.serenity.domain.model.StressNudge
import com.serenity.service.TimerStateHolder
import org.json.JSONObject
import java.time.Instant

/**
 * Receives stress signal messages from the WearOS companion app.
 * Path: /stress/alert  — payload: JSON { hr: Int, stress: Float }
 *
 * If the user is not already meditating, posts a nudge notification
 * with a quick-start action for a 2-minute calming session.
 */
class PhoneWearableListenerService : WearableListenerService() {

    companion object {
        const val PATH_STRESS = "/stress/alert"
        const val CHANNEL_ID  = "stress_nudge"
        const val NOTIF_ID    = 3001
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PATH_STRESS) return

        val payload = String(event.data)
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return
        val hr = json.optInt("hr", 0)
        val stress = json.optDouble("stress", 0.0).toFloat()

        val nudge = StressNudge(heartRate = hr, stressLevel = stress, receivedAt = Instant.now())
        TimerStateHolder.emitNudge(nudge)

        // Only nudge if timer is not already running
        if (TimerStateHolder.state.value !is com.serenity.domain.model.TimerState.Running) {
            showNudgeNotification(hr, stress)
        }
    }

    private fun showNudgeNotification(hr: Int, stress: Float) {
        val nm = getSystemService(NotificationManager::class.java)
        ensureChannel(nm)

        // Deep-link to a 2-min calming preset
        val calmIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", "calm_start")
            putExtra("calm_duration_sec", 120)
        }
        val pi = PendingIntent.getActivity(this, 0, calmIntent, PendingIntent.FLAG_IMMUTABLE)

        val body = when {
            stress > 0.75f -> "Your heart rate ($hr bpm) suggests high stress. A 2-min breathing session can help."
            stress > 0.5f  -> "Elevated stress detected ($hr bpm). Take a mindful moment?"
            else           -> "Your watch noticed some tension. A short pause might help."
        }

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_meditation)
            .setContentTitle("Time to breathe 🧘")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .addAction(R.drawable.ic_play, "Start 2-min calm", pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIF_ID, notif)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Stress Nudge",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Gentle nudge when your watch detects elevated stress" }
        )
    }
}
