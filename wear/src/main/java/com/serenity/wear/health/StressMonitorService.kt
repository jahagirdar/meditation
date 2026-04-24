package com.serenity.wear.health

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import com.google.android.gms.wearable.Wearable
import com.serenity.wear.MainActivity
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Foreground service that subscribes to heart-rate and derived stress metrics
 * via Health Services, then sends a Wearable message to the paired phone
 * when elevated stress is detected.
 */
class StressMonitorService : Service() {

    companion object {
        const val CHANNEL_ID   = "stress_monitor"
        const val NOTIF_ID     = 9001
        const val PATH_STRESS  = "/stress/alert"

        // Thresholds for nudge
        private const val HIGH_HR_THRESHOLD    = 85    // bpm
        private const val STRESS_THRESHOLD     = 0.6f  // normalised 0–1

        // Minimum gap between nudges (ms)
        private const val NUDGE_COOLDOWN_MS    = 10 * 60 * 1000L  // 10 minutes
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastNudgeSentAt = 0L

    // Sliding window for HR smoothing (last 10 readings)
    private val hrWindow = ArrayDeque<Double>(10)

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            // Could show watch UI indicator here
        }

        override fun onDataReceived(data: DataPointContainer) {
            // Heart rate samples
            data.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                val hr = sample.value
                hrWindow.addLast(hr)
                if (hrWindow.size > 10) hrWindow.removeFirst()

                val avgHr = hrWindow.average()
                evaluateStress(hr = avgHr.toInt(), rawHr = hr)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        subscribeToHeartRate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Health Services ──

    private fun subscribeToHeartRate() {
        scope.launch {
            try {
                val measureClient = HealthServices.getClient(this@StressMonitorService)
                    .measureClient
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
            } catch (e: Exception) {
                // Health Services not available on this device
                stopSelf()
            }
        }
    }

    // ── Stress evaluation ──

    /**
     * Simple heuristic: if average HR stays above threshold for the window,
     * treat it as elevated stress and notify.
     *
     * In a production app you'd use HealthServices passive monitoring with
     * a PassiveListenerService and proper PassiveGoal thresholds.
     */
    private fun evaluateStress(hr: Int, rawHr: Double) {
        if (hrWindow.size < 5) return  // need enough readings
        val allElevated = hrWindow.all { it > HIGH_HR_THRESHOLD }
        if (!allElevated) return

        val now = System.currentTimeMillis()
        if (now - lastNudgeSentAt < NUDGE_COOLDOWN_MS) return

        // Normalise stress 0–1 based on how far HR is above threshold
        val stressLevel = ((hr - HIGH_HR_THRESHOLD).toFloat() / 40f).coerceIn(0f, 1f)
        if (stressLevel < STRESS_THRESHOLD) return

        lastNudgeSentAt = now
        sendStressAlertToPhone(hr, stressLevel)
    }

    private fun sendStressAlertToPhone(hr: Int, stress: Float) {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@StressMonitorService)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) return@launch

                val payload = JSONObject().apply {
                    put("hr", hr)
                    put("stress", stress)
                }.toString().toByteArray()

                val msgClient = Wearable.getMessageClient(this@StressMonitorService)
                nodes.forEach { node ->
                    msgClient.sendMessage(node.id, PATH_STRESS, payload).await()
                }

                // Show local watch notification too
                showWatchNotification(hr)
            } catch (e: Exception) {
                // Node not reachable — ignore
            }
        }
    }

    private fun showWatchNotification(hr: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Breathe 🧘")
            .setContentText("Heart rate $hr bpm — a calm session might help")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID + 1, notif)
    }

    // ── Notification ──

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Stress Monitor",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Monitors heart rate in the background"
                setSound(null, null)
            }
        )
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Serenity")
            .setContentText("Monitoring for stress signals")
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

// Helper extension for Task<T> await
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.tasks.await()
