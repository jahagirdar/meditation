package com.serenity.ui.session

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.domain.model.*
import com.serenity.service.MeditationTimerService
import com.serenity.service.TimerActions

@Composable
fun SessionScreen(
    preset: Preset,
    onComplete: (actualSec: Int) -> Unit,
    onExit: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val context = LocalContext.current
    val showElapsed by viewModel.showElapsed.collectAsState()
    val breathingEnabled by viewModel.breathingEnabled.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }

    // Start the service when entering this screen
    LaunchedEffect(Unit) {
        viewModel.startSession(preset, context)
    }

    // Handle completion
    LaunchedEffect(timerState) {
        if (timerState is TimerState.Completed) {
            onComplete((timerState as TimerState.Completed).actualDurationSec)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = timerState) {
            is TimerState.Running -> ActiveTimerContent(
                state = s,
                showElapsed = showElapsed,
                breathingEnabled = breathingEnabled,
                onPause = { viewModel.pauseSession(context) },
                onStop  = { showStopDialog = true },
            )
            is TimerState.Paused  -> PausedContent(
                state = s,
                onResume = { viewModel.resumeSession(context) },
                onStop   = { showStopDialog = true },
            )
            is TimerState.Completed -> {
                // Handled by LaunchedEffect
                Box {}
            }
            else -> {
                // Idle — shouldn't happen; redirect
                LaunchedEffect(Unit) { onExit() }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("End session early?") },
            text = { Text("Your progress so far will still be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopSession(context)
                    showStopDialog = false
                    onExit()
                }) { Text("End session", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Keep going") }
            },
        )
    }
}

// ──────────────────────────────────────────────
// Active timer content
// ──────────────────────────────────────────────

@Composable
private fun ActiveTimerContent(
    state: TimerState.Running,
    showElapsed: Boolean,
    breathingEnabled: Boolean,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val displaySec = if (showElapsed) state.elapsedSec else state.remainingSec
    val progress   = if (state.totalSec > 0)
        state.elapsedSec.toFloat() / state.totalSec.toFloat() else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        // Phase label
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { phase ->
            Text(
                text = when (phase) {
                    TimerPhase.WARMUP    -> "Warming up…"
                    TimerPhase.MEDITATION -> "Meditating"
                    TimerPhase.COOLDOWN  -> "Winding down…"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(48.dp))

        // Breathing circle + progress arc + timer
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            // Outer progress arc
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            // Breathing animation circle
            if (breathingEnabled && state.phase == TimerPhase.MEDITATION) {
                BreathingCircle()
            }

            // Timer text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(displaySec),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (showElapsed) "elapsed" else "remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(64.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }

            FloatingActionButton(
                onClick = onPause,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(Icons.Default.Pause, "Pause", modifier = Modifier.size(32.dp))
            }
        }
    }
}

// ──────────────────────────────────────────────
// Paused content
// ──────────────────────────────────────────────

@Composable
private fun PausedContent(
    state: TimerState.Paused,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text("Paused", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text(
            formatTime(state.remainingSec),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Thin),
        )
        Text("remaining", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(64.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onStop) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("End")
            }
            Button(
                onClick = onResume,
                modifier = Modifier.height(56.dp).width(160.dp),
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Resume", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ──────────────────────────────────────────────
// Breathing animation (4-7-8 rhythm)
// ──────────────────────────────────────────────

@Composable
fun BreathingCircle() {
    // 4s inhale, 7s hold, 8s exhale = 19s cycle
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 19_000
                0.5f  at 0           using FastOutSlowInEasing  // start inhale
                0.85f at 4_000       using LinearEasing          // hold
                0.85f at 11_000      using FastOutSlowInEasing   // hold ends
                0.5f  at 19_000      using LinearEasing          // exhale
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathScale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue  = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathAlpha",
    )

    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(220.dp).scale(scale),
    ) {
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0xFF5B7FA6),
            alpha = alpha,
            radius = size.minDimension / 2,
        )
    }
}

// ──────────────────────────────────────────────
// Completion sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCompleteSheet(
    actualSec: Int,
    streak: Int,
    onDone: (notes: String?) -> Unit,
) {
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { onDone(null) },
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🙏", fontSize = 40.sp)
            Text("Session complete", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatChip(label = "Duration", value = formatTime(actualSec))
                if (streak > 0) StatChip(label = "Streak", value = "🔥 $streak days")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Add a note about this session… (optional)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
            )

            Button(
                onClick = { onDone(notes.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Done", style = MaterialTheme.typography.titleMedium) }

            TextButton(onClick = { onDone(null) }) { Text("Skip") }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
