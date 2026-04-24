package com.serenity.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.serenity.wear.health.StressMonitorService
import com.serenity.wear.ui.WearMainScreen
import com.serenity.wear.ui.WearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) startStressMonitor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSensorsIfNeeded()

        setContent {
            WearTheme {
                WearMainScreen(
                    onStartMonitor = { requestSensorsIfNeeded() }
                )
            }
        }
    }

    private fun requestSensorsIfNeeded() {
        val perms = arrayOf(
            Manifest.permission.BODY_SENSORS,
        )
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) startStressMonitor()
        else permissionLauncher.launch(perms)
    }

    private fun startStressMonitor() {
        startForegroundService(Intent(this, StressMonitorService::class.java))
    }
}
