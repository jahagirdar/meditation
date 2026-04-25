package com.serenity.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*

// ──────────────────────────────────────────────
// Theme
// ──────────────────────────────────────────────

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary         = Color(0xFF5B7FA6),
            onPrimary       = Color.White,
            background      = Color(0xFF000000),
            onBackground    = Color(0xFFE3E1DC),
            surface         = Color(0xFF1A1C1E),
            onSurface       = Color(0xFFE3E1DC),
        ),
        content = content,
    )
}

// ──────────────────────────────────────────────
// Main watch screen
// ──────────────────────────────────────────────

@Composable
fun WearMainScreen(
    onStartMonitor: () -> Unit,
    viewModel: WearViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 12.dp),
    ) {
        item {
            Text(
                "Serenity",
                style = MaterialTheme.typography.title2.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 18.sp,
                ),
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
            )
        }

        item { HeartRateTile(bpm = state.currentHr) }
        item { StressLevelTile(stressLevel = state.stressLevel) }
        item {
            StatusTile(
                isMonitoring     = state.isMonitoring,
                lastNudgeSentAgo = state.lastNudgeSentAgoMin,
                onStartMonitor   = onStartMonitor,
            )
        }

        item {
            Text(
                "Open Serenity on your phone to start a session",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Heart rate tile
// ──────────────────────────────────────────────

@Composable
private fun HeartRateTile(bpm: Int?) {
    val bpmColor = when {
        bpm == null -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        bpm > 90    -> Color(0xFFEF5350)
        bpm > 75    -> Color(0xFFFF9800)
        else        -> Color(0xFF4CAF50)
    }
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        label = {
            Column {
                Text(
                    text = if (bpm != null) "$bpm bpm" else "—",
                    style = MaterialTheme.typography.title1.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = bpmColor,
                )
                Text(
                    "Heart rate",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        icon = { Text("❤️", fontSize = 20.sp) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface),
    )
}

// ──────────────────────────────────────────────
// Stress level tile — uses Canvas progress bar
// (LinearProgressIndicator is not in Wear Compose)
// ──────────────────────────────────────────────

@Composable
private fun StressLevelTile(stressLevel: Float?) {
    val label = when {
        stressLevel == null   -> "No data"
        stressLevel >= 0.75f  -> "High"
        stressLevel >= 0.50f  -> "Moderate"
        stressLevel >= 0.25f  -> "Low"
        else                  -> "Calm"
    }
    val barColor = when {
        stressLevel == null   -> MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        stressLevel >= 0.75f  -> Color(0xFFEF5350)
        stressLevel >= 0.50f  -> Color(0xFFFF9800)
        else                  -> Color(0xFF4CAF50)
    }
    val trackColor = MaterialTheme.colors.surface

    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        label = {
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor,
                )
                Text(
                    "Stress signal",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                if (stressLevel != null) {
                    Spacer(Modifier.height(5.dp))
                    // Canvas-drawn progress bar (LinearProgressIndicator
                    // is M3 phone only — not available in Wear Compose)
                    val progress = stressLevel
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                    ) {
                        val radius = CornerRadius(size.height / 2)
                        // Track
                        drawRoundRect(
                            color        = trackColor,
                            size         = size,
                            cornerRadius = radius,
                        )
                        // Fill
                        drawRoundRect(
                            color        = barColor,
                            size         = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        },
        icon = { Text("🧠", fontSize = 18.sp) },
        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface),
    )
}

// ──────────────────────────────────────────────
// Status tile
// ──────────────────────────────────────────────

@Composable
private fun StatusTile(
    isMonitoring: Boolean,
    lastNudgeSentAgo: Int?,
    onStartMonitor: () -> Unit,
) {
    if (isMonitoring) {
        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            label = {
                Column {
                    Text(
                        "Monitoring active",
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFF4CAF50),
                    )
                    Text(
                        if (lastNudgeSentAgo != null) "Last nudge: ${lastNudgeSentAgo}m ago"
                        else "Watching for stress signals",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            },
            icon = { Text("✅", fontSize = 16.sp) },
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.surface),
        )
    } else {
        Button(
            onClick = onStartMonitor,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
        ) {
            Text("Enable monitoring", style = MaterialTheme.typography.button)
        }
    }
}

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

data class WearUiState(
    val currentHr: Int? = null,
    val stressLevel: Float? = null,
    val isMonitoring: Boolean = false,
    val lastNudgeSentAgoMin: Int? = null,
)

class WearViewModel : androidx.lifecycle.ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(WearUiState())
    val state: kotlinx.coroutines.flow.StateFlow<WearUiState> = _state

    fun onHeartRate(bpm: Int, stressLevel: Float) {
        _state.value = _state.value.copy(
            currentHr    = bpm,
            stressLevel  = stressLevel,
            isMonitoring = true,
        )
    }

    fun onMonitoringStarted() {
        _state.value = _state.value.copy(isMonitoring = true)
    }

    fun onNudgeSent() {
        _state.value = _state.value.copy(lastNudgeSentAgoMin = 0)
    }
}
