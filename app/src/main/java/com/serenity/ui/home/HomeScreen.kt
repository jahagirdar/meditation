package com.serenity.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.crash.CrashHandler
import com.serenity.domain.model.*
import com.serenity.ui.components.PresetCard
import com.serenity.ui.components.TimerConfigSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: (Preset) -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAssessment: () -> Unit,
    onNavigatePranayama: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state    by viewModel.state.collectAsState()
    val context  = LocalContext.current
    val hasCrash = remember { CrashHandler.hasUnreadReport(context) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfigSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serenity", fontWeight = FontWeight.Light, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = onNavigateHistory) {
                        Icon(Icons.Default.BarChart, "History")
                    }
                    BadgedBox(
                        badge = {
                            if (hasCrash) Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        }
                    ) {
                        IconButton(onClick = onNavigateSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Main timer card ──
            MainTimerCard(
                preset            = state.activePreset,
                todayMinutes      = state.todayMinutes,
                dailyGoalMinutes  = state.dailyGoalMinutes,
                currentStreak     = state.currentStreak,
                onStart           = { onStartSession(state.activePreset) },
                onEditConfig      = { showConfigSheet = true },
            )

            // ── Quick duration chips ──
            QuickDurationRow(
                selected = state.activePreset.durationSec,
                onSelect = { viewModel.setDuration(it) },
            )

            Spacer(Modifier.height(20.dp))

            // ── Pranayama entry ──
            PranayamaEntryCard(onClick = onNavigatePranayama)

            Spacer(Modifier.height(20.dp))

            // ── Presets ──
            if (state.presets.isNotEmpty()) {
                Text(
                    "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.presets) { preset ->
                        PresetCard(
                            preset    = preset,
                            isActive  = preset.id == state.activePreset.id,
                            onSelect  = { viewModel.selectPreset(preset) },
                            onStart   = { onStartSession(preset) },
                            onDelete  = { viewModel.deletePreset(preset) },
                        )
                    }
                    item {
                        AddPresetCard(onClick = { showConfigSheet = true })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Daily self-assessment nudge ──
            AssessmentNudgeCard(
                answered = state.todayAssessmentAnswered,
                total    = 20,
                onClick  = onNavigateAssessment,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showConfigSheet) {
        TimerConfigSheet(
            preset       = state.activePreset,
            sheetState   = bottomSheetState,
            onDismiss    = { showConfigSheet = false },
            onSave       = { updated -> viewModel.updatePreset(updated); showConfigSheet = false },
            onSaveAsNew  = { viewModel.saveNewPreset(it); showConfigSheet = false },
        )
    }
}

// ── Main timer card ──

@Composable
private fun MainTimerCard(
    preset: Preset,
    todayMinutes: Int,
    dailyGoalMinutes: Int,
    currentStreak: Int,
    onStart: () -> Unit,
    onEditConfig: () -> Unit,
) {
    val durationMin = preset.durationSec / 60
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (currentStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🔥", fontSize = 16.sp)
                    Text("$currentStreak day streak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "${durationMin}m",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Thin, fontSize = 80.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(preset.name.ifBlank { "Custom Session" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (preset.warmupSec > 0) ConfigChip("${preset.warmupSec / 60}m warmup")
                if (preset.intervalOption != null) ConfigChip("⏱ ${preset.intervalOption.displayName}")
                if (preset.ambientSound != AmbientSound.NONE) ConfigChip("♪ ${preset.ambientSound.displayName}")
                if (preset.silentMode) ConfigChip("silent")
            }
            Spacer(Modifier.height(24.dp))
            val goalProgress = (todayMinutes.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
            if (dailyGoalMinutes > 0) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today: ${todayMinutes}m / ${dailyGoalMinutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goalProgress >= 1f) Text("✓ Goal met",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50))
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = if (goalProgress >= 1f) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(20.dp))
            }
            Button(onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Begin Session", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onEditConfig) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Configure")
            }
        }
    }
}

@Composable
private fun ConfigChip(label: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Quick duration chips ──

@Composable
private fun QuickDurationRow(selected: Int, onSelect: (Int) -> Unit) {
    val durations = listOf(5, 10, 15, 20, 30, 45, 60)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(durations) { mins ->
            FilterChip(
                selected = selected == mins * 60,
                onClick  = { onSelect(mins * 60) },
                label    = { Text("${mins}m") },
            )
        }
    }
}

// ── Pranayama entry card ──

@Composable
private fun PranayamaEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("🫁", fontSize = 36.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Guided Pranayama",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("Box · 4-7-8 · Nadi Shodhana · Kapalabhati · Bhramari & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("7 techniques with animated breath guidance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Assessment nudge ──

@Composable
private fun AssessmentNudgeCard(answered: Int, total: Int, onClick: () -> Unit) {
    val pct = answered.toFloat() / total.toFloat()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📋", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily Self-Assessment", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    if (answered == 0) "Not started today"
                    else if (answered == total) "Complete ✓"
                    else "$answered of $total answered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (answered > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { pct },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddPresetCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(130.dp).height(140.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Add, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Add preset", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
