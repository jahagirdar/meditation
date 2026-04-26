package com.serenity.ui.pranayama

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.domain.model.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PranayamaPickerScreen(
    onBack: () -> Unit,
    onStartSession: (technique: String, rounds: Int) -> Unit,
    viewModel: PranayamaViewModel,
) {
    val state by viewModel.pickerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pranayama", style = MaterialTheme.typography.titleLarge)
                        Text("Guided breath work",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Rounds selector
                    RoundsSelector(
                        rounds = state.rounds,
                        onDecrease = { viewModel.setRounds(state.rounds - 1) },
                        onIncrease = { viewModel.setRounds(state.rounds + 1) },
                    )
                    Button(
                        onClick = { onStartSession(state.selectedTechnique.name, state.rounds) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Begin ${state.rounds} rounds of ${state.selectedTechnique.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Stats summary
            if (state.totalPranayamaMinutes > 0) {
                item {
                    PranayamaStatsBanner(
                        totalMinutes  = state.totalPranayamaMinutes,
                        recentCount   = state.recentSessions.size,
                    )
                }
            }

            // Technique grid
            item {
                Text("Choose a technique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp))
            }

            // 2-column grid via chunked list
            items(PranayamaTechnique.entries.chunked(2)) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { technique ->
                        TechniqueCard(
                            technique  = technique,
                            isSelected = state.selectedTechnique == technique,
                            onClick    = { viewModel.selectTechnique(technique) },
                            modifier   = Modifier.weight(1f),
                        )
                    }
                    // Pad last row if odd number
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Selected technique details
            item {
                AnimatedContent(
                    targetState = state.selectedTechnique,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "technique_detail",
                ) { technique ->
                    TechniqueDetailCard(technique = technique)
                }
            }

            // Recent sessions
            if (state.recentSessions.isNotEmpty()) {
                item {
                    Text("Recent sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
                items(state.recentSessions) { session ->
                    RecentSessionRow(session = session)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
// Technique card (grid item)
// ──────────────────────────────────────────────

@Composable
private fun TechniqueCard(
    technique: PranayamaTechnique,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val diffColor = Color(technique.difficulty.color)

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(technique.emoji, fontSize = 28.sp)
            Column {
                Text(
                    technique.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Text(
                    technique.sanskritName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                // Difficulty badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = diffColor.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentSize(),
                ) {
                    Text(
                        technique.difficulty.label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = diffColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Technique detail card — phases + description
// ──────────────────────────────────────────────

@Composable
private fun TechniqueDetailCard(technique: PranayamaTechnique) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(technique.emoji, fontSize = 24.sp)
                Column {
                    Text(technique.displayName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(technique.tagline, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(technique.description, style = MaterialTheme.typography.bodyMedium)

            // Phase strip
            Text("Breath pattern", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)

            PhaseStrip(phases = technique.phases)

            // Benefits
            Text("Benefits", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            technique.benefits.forEach { benefit ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Text(benefit, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Practitioner notes
            if (technique.notesForPractitioner.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Text(technique.notesForPractitioner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Vyahriti chant sequence
            VyahritiSequencePreview()
        }
    }
}

// ──────────────────────────────────────────────
// Vyahriti sequence preview in picker
// ──────────────────────────────────────────────

@Composable
private fun VyahritiSequencePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Chant sequence — one per round",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("The Sapta Vyahritis cycle every 7 rounds. " +
             "The Gayatri Mantra is shown when each cycle of 7 completes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Vyahriti.entries.forEachIndexed { i, v ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Round number badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }
                }
                // Sanskrit
                Text(v.sanskrit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(90.dp))
                // Plane
                Text(v.plane,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Gayatri marker
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFC48B40).copy(alpha = 0.18f),
                modifier = Modifier.size(22.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("✦", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC48B40))
                }
            }
            Text("ॐ तत्सवितुर्वरेण्यं…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC48B40))
            Text("Gayatri — shown after round 7, 14…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ──────────────────────────────────────────────
// Phase strip
// ──────────────────────────────────────────────

@Composable
fun PhaseStrip(phases: List<PhaseSpec>) {
    val totalSec = phases.sumOf { it.durationSec }.toFloat().coerceAtLeast(1f)
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        phases.forEach { spec ->
            val weight = spec.durationSec / totalSec
            val color  = Color(spec.phase.color)
            Box(
                modifier = Modifier
                    .weight(weight.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.25f))
                    .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = spec.countLabel ?: "${spec.durationSec}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
    // Phase labels row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        phases.forEach { spec ->
            val weight = (spec.durationSec / totalSec).coerceAtLeast(0.05f)
            Text(
                text = spec.phase.label.take(7),
                modifier = Modifier.weight(weight),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Rounds selector
// ──────────────────────────────────────────────

@Composable
private fun RoundsSelector(rounds: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease) {
            Icon(Icons.Default.Remove, "Fewer rounds")
        }
        Text(
            "$rounds rounds",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrease) {
            Icon(Icons.Default.Add, "More rounds")
        }
    }
}

// ──────────────────────────────────────────────
// Stats banner
// ──────────────────────────────────────────────

@Composable
private fun PranayamaStatsBanner(totalMinutes: Int, recentCount: Int) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatPill("🫁", "${totalMinutes}m", "Total breath work")
            StatPill("🔄", "$recentCount", "Recent sessions")
        }
    }
}

@Composable
private fun StatPill(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ──────────────────────────────────────────────
// Recent session row
// ──────────────────────────────────────────────

@Composable
private fun RecentSessionRow(session: PranayamaSession) {
    val dateStr = session.startedAt
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(session.technique.emoji, fontSize = 22.sp)
                Column {
                    Text(session.technique.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text("$dateStr · ${session.roundsCompleted} rounds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${session.durationSec / 60}m",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
