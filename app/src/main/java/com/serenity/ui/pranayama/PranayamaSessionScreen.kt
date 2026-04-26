package com.serenity.ui.pranayama

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.domain.model.*

// ──────────────────────────────────────────────
// Root screen
// ──────────────────────────────────────────────

@Composable
fun PranayamaSessionScreen(
    onComplete: (PranayamaSession) -> Unit,
    onExit: () -> Unit,
    viewModel: PranayamaViewModel,
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }
    var showGayatriOverlay by remember { mutableStateOf(false) }
    var lastCycleSeenAt by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.sessionComplete.collect { session -> onComplete(session) }
    }

    // Detect when a fresh Gayatri cycle boundary is crossed
    val s = sessionState
    LaunchedEffect(s?.currentRound) {
        if (s != null && s.completesGayatriCycle && s.currentRound != lastCycleSeenAt
            && s.phaseRemainingSec == s.currentPhase.durationSec  // first tick of the round
        ) {
            lastCycleSeenAt = s.currentRound
            showGayatriOverlay = true
        }
    }

    // Session is started by the nav before navigating here, so sessionState
    // should be non-null immediately. Show a spinner for safety rather than exiting.
    if (sessionState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when {
            s == null -> {}
            s.isComplete -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            s.isPaused -> PausedOverlay(
                state    = s,
                onResume = { viewModel.resumeSession() },
                onStop   = { viewModel.stopSession(); onExit() },
            )
            else -> ActiveBreathingContent(
                state   = s,
                onPause = { viewModel.pauseSession() },
                onStop  = { showStopDialog = true },
            )
        }

        // Gayatri cycle overlay (auto-dismisses after 8 seconds)
        if (showGayatriOverlay) {
            GayatriCycleOverlay(onDismiss = { showGayatriOverlay = false })
            LaunchedEffect(showGayatriOverlay) {
                kotlinx.coroutines.delay(8_000)
                showGayatriOverlay = false
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("End practice early?") },
            text  = {
                val rounds = s?.let { "${it.currentRound - 1} of ${it.totalRounds} rounds" } ?: ""
                Text("$rounds completed will be saved.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopSession(); showStopDialog = false; onExit()
                }) { Text("End", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Keep going") }
            },
        )
    }
}

// ──────────────────────────────────────────────
// Active breathing content
// ──────────────────────────────────────────────

@Composable
private fun ActiveBreathingContent(
    state: PranayamaSessionState,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val phaseSpec  = state.currentPhase
    val phaseColor = Color(phaseSpec.phase.color)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Top: technique + round counter ───────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(
                "${state.technique.emoji}  ${state.technique.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RoundProgressRow(
                current = state.currentRound,
                total   = state.totalRounds,
                color   = phaseColor,
            )
        }

        // ── Centre: breathing circle ──────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            BreathingCircleAnimated(
                phase         = phaseSpec.phase,
                phaseProgress = state.phaseProgress,
                phaseColor    = phaseColor,
                isRapid       = phaseSpec.isRapid,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = phaseSpec.phase.label,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "phase_label",
                ) { label ->
                    Text(label,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                        color = phaseColor, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                if (phaseSpec.isRapid) {
                    Text(phaseSpec.countLabel ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = phaseColor.copy(alpha = 0.8f))
                } else {
                    AnimatedContent(
                        targetState = state.phaseRemainingSec,
                        transitionSpec = {
                            slideInVertically { it / 2 } + fadeIn() togetherWith
                            slideOutVertically { -it / 2 } + fadeOut()
                        },
                        label = "countdown",
                    ) { secs ->
                        Text("$secs",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Thin, fontSize = 56.sp),
                            color = phaseColor.copy(alpha = 0.9f))
                    }
                }
            }
        }

        // ── Vyahriti chant card ───────────────────
        VyahritiCard(
            vyahriti   = state.currentVyahriti,
            roundIndex = (state.currentRound - 1) % 7,  // 0-based position in cycle
        )

        // ── Phase instruction ─────────────────────
        AnimatedContent(
            targetState = phaseSpec.phase.instruction,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
            label = "instruction",
        ) { instruction ->
            Text(instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp))
        }

        // ── Controls ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Stop, "Stop",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp))
            }
            FloatingActionButton(
                onClick = onPause,
                containerColor = phaseColor,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(Icons.Default.Pause, "Pause",
                    tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Box(Modifier.size(48.dp))
        }
    }
}

// ──────────────────────────────────────────────
// Vyahriti chant card — shown below the circle
// ──────────────────────────────────────────────

@Composable
private fun VyahritiCard(vyahriti: Vyahriti, roundIndex: Int) {
    AnimatedContent(
        targetState = vyahriti,
        transitionSpec = {
            (fadeIn(tween(600)) + slideInVertically { it / 3 }) togetherWith
            (fadeOut(tween(400)) + slideOutVertically { -it / 3 })
        },
        label = "vyahriti",
    ) { v ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Cycle position dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(7) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == roundIndex) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == roundIndex) MaterialTheme.colorScheme.primary
                                    else if (i < roundIndex)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Sanskrit
                Text(
                    v.sanskrit,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize   = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                // Transliteration
                Text(
                    v.transliteration,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Meaning
                Text(
                    v.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                // Plane label
                Text(
                    v.plane,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Gayatri cycle overlay — shown for 8 s when round % 7 == 0
// ──────────────────────────────────────────────

@Composable
private fun GayatriCycleOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.93f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("✨", fontSize = 36.sp)
            Text(
                "Sapta Vyahritis complete",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Divider(
                modifier = Modifier.width(60.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                thickness = 1.dp,
            )

            // Gayatri Sanskrit
            Text(
                GayatriMantra.sanskrit,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight  = 40.sp,
                    fontSize    = 22.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            // Transliteration
            Text(
                GayatriMantra.transliteration,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Divider(
                modifier = Modifier.width(60.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 1.dp,
            )

            // Meaning
            Text(
                GayatriMantra.meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Tap to continue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Animated breathing circle (unchanged logic)
// ──────────────────────────────────────────────

@Composable
private fun BreathingCircleAnimated(
    phase: BreathPhase,
    phaseProgress: Float,
    phaseColor: Color,
    isRapid: Boolean,
) {
    val targetScale = when (phase) {
        BreathPhase.INHALE,
        BreathPhase.INHALE_L,
        BreathPhase.INHALE_R  -> 0.95f
        BreathPhase.HOLD_IN   -> 0.95f
        BreathPhase.EXHALE,
        BreathPhase.EXHALE_L,
        BreathPhase.EXHALE_R,
        BreathPhase.HUMMING   -> 0.45f
        BreathPhase.HOLD_OUT  -> 0.45f
        BreathPhase.PUMP      -> 0.55f
        BreathPhase.PREPARE   -> 0.60f
    }
    val animatedScale by animateFloatAsState(
        targetValue = if (isRapid) {
            if ((phaseProgress * 10).toInt() % 2 == 0) 0.65f else 0.50f
        } else targetScale,
        animationSpec = when (phase) {
            BreathPhase.INHALE,
            BreathPhase.INHALE_L,
            BreathPhase.INHALE_R,
            BreathPhase.EXHALE,
            BreathPhase.EXHALE_L,
            BreathPhase.EXHALE_R,
            BreathPhase.HUMMING  -> tween(
                durationMillis = (phaseProgress * 1000).toInt().coerceAtLeast(800),
                easing = EaseInOutCubic)
            else -> tween(400, easing = LinearEasing)
        },
        label = "breathScale",
    )
    val holdPulse by if (phase == BreathPhase.HOLD_IN || phase == BreathPhase.HOLD_OUT) {
        rememberInfiniteTransition(label = "hold").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing),
                RepeatMode.Reverse),
            label = "hold_alpha",
        )
    } else remember { mutableStateOf(0f) }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(color = phaseColor,
                radius = r * animatedScale + if (phase == BreathPhase.HOLD_IN ||
                    phase == BreathPhase.HOLD_OUT) holdPulse * 8f else 0f,
                alpha = 0.10f + holdPulse * 0.05f,
                style = Stroke(width = 2.dp.toPx()))
        }
        Canvas(modifier = Modifier.fillMaxSize().scale(animatedScale)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(phaseColor.copy(alpha = 0.22f), phaseColor.copy(alpha = 0.06f)),
                    Offset(size.width / 2, size.height / 2), r),
                radius = r)
            drawCircle(color = phaseColor, radius = r, alpha = 0.55f,
                style = Stroke(width = 2.dp.toPx()))
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            val r = size.minDimension / 2f - 6.dp.toPx()
            drawArc(color = phaseColor, startAngle = -90f,
                sweepAngle = phaseProgress * 360f, useCenter = false, style = stroke, alpha = 0.8f)
            drawArc(color = phaseColor, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, style = stroke, alpha = 0.12f)
        }
    }
}

// ──────────────────────────────────────────────
// Round progress row
// ──────────────────────────────────────────────

@Composable
private fun RoundProgressRow(current: Int, total: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Round $current of $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (total <= 12) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(total) { i ->
                    val isCurrent = i == current - 1
                    val isDone    = i < current - 1
                    Box(modifier = Modifier
                        .size(if (isCurrent) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(when {
                            isCurrent -> color
                            isDone    -> color.copy(alpha = 0.5f)
                            else      -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }))
                }
            }
        } else {
            LinearProgressIndicator(
                progress = { (current - 1).toFloat() / total.toFloat() },
                modifier = Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Paused overlay
// ──────────────────────────────────────────────

@Composable
private fun PausedOverlay(
    state: PranayamaSessionState,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.technique.emoji, fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Paused", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text("Round ${state.currentRound} of ${state.totalRounds} · ${state.currentVyahriti.transliteration}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Resume", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Text("End session")
        }
    }
}

// ──────────────────────────────────────────────
// Session complete screen
// ──────────────────────────────────────────────

@Composable
fun PranayamaCompleteScreen(
    session: PranayamaSession,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🙏", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))
        Text("Practice complete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(session.technique.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CompleteStat("🔄", "${session.roundsCompleted}", "Rounds")
            CompleteStat("⏱",  "${session.durationSec / 60}m ${session.durationSec % 60}s", "Duration")
            CompleteStat(session.technique.emoji, session.technique.difficulty.label, "Level")
        }

        Spacer(Modifier.height(28.dp))

        // Show last vyahriti reached
        val lastVyahriti = Vyahriti.forRound(session.roundsCompleted)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Last chant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(lastVyahriti.sanskrit,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center)
                Text(lastVyahriti.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "May all beings be happy… be peaceful… be liberated…",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Done", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CompleteStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

