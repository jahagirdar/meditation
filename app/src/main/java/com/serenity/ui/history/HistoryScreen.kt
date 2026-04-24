package com.serenity.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.domain.model.Session
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Stats cards
            item {
                StatsGrid(
                    streak = state.stats?.currentStreak ?: 0,
                    longestStreak = state.stats?.longestStreak ?: 0,
                    totalSessions = state.stats?.totalSessions ?: 0,
                    totalMinutes = state.stats?.totalMinutes ?: 0,
                    avgMin = state.stats?.avgDurationMinutes ?: 0,
                    todayMin = state.stats?.todayMinutes ?: 0,
                    goalMin = state.stats?.dailyGoalMinutes ?: 10,
                )
            }

            // Weekly chart
            item {
                WeeklyChart(counts = state.stats?.weeklySessionCounts ?: List(7) { 0 })
            }

            // Session list header
            item {
                Text("Sessions", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }

            if (state.sessions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No sessions yet. Start meditating!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.sessions, key = { it.id.toString() }) { session ->
                    SessionRow(session = session)
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(
    streak: Int, longestStreak: Int, totalSessions: Int,
    totalMinutes: Int, avgMin: Int, todayMin: Int, goalMin: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("🔥 Streak", "$streak days", Modifier.weight(1f))
            StatCard("🏆 Best", "$longestStreak days", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("🧘 Sessions", "$totalSessions", Modifier.weight(1f))
            StatCard("⏱ Total", "${totalMinutes}m", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("📊 Avg session", "${avgMin}m", Modifier.weight(1f))
            StatCard("Today", "${todayMin}m / ${goalMin}m", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WeeklyChart(counts: List<Int>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This week", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                days.forEachIndexed { i, day ->
                    val hasSession = (counts.getOrNull(i) ?: 0) > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasSession) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                ) {}
                                Text("✓", color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(day, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    val dateStr = session.startedAt.atZone(ZoneId.systemDefault())
        .format(formatter)
    val durationMin = session.actualDurationSec / 60

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(session.presetName ?: "Session",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(dateStr, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!session.notes.isNullOrBlank()) {
                    Text(session.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1)
                }
            }
            Text("${durationMin}m", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
