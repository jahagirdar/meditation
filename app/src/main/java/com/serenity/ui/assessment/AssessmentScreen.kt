package com.serenity.ui.assessment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ──────────────────────────────────────────────
// Root screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreen(
    onBack: () -> Unit,
    viewModel: AssessmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val assessment by viewModel.selectedAssessment.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daily Self-Assessment", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "An effort to know whether we are progressing in Dhamma",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Date navigator
                    IconButton(onClick = {
                        viewModel.selectDate(state.selectedDate.minusDays(1))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day")
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        val isToday = state.selectedDate == LocalDate.now()
                        Text(
                            if (isToday) "Today"
                            else state.selectedDate.format(DateTimeFormatter.ofPattern("d MMM")),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectDate(state.selectedDate.plusDays(1)) },
                        enabled = state.selectedDate < LocalDate.now(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day")
                    }
                },
            )
        },
        bottomBar = {
            AssessmentBottomBar(
                assessment = assessment,
                onClear = { showClearDialog = true },
                onMarkAll = { viewModel.markAllYes() },
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
            // Progress summary card
            item {
                AssessmentProgressCard(assessment = assessment)
            }

            // Month heatmap
            item {
                MonthHeatmap(
                    month = state.selectedMonth,
                    assessments = state.monthAssessments,
                    selectedDate = state.selectedDate,
                    onSelectDate = { viewModel.selectDate(it) },
                    onPrevMonth = { viewModel.selectMonth(state.selectedMonth.minusMonths(1)) },
                    onNextMonth = {
                        if (state.selectedMonth < YearMonth.now())
                            viewModel.selectMonth(state.selectedMonth.plusMonths(1))
                    },
                )
            }

            // Parameters grouped by category
            viewModel.parametersByCategory.forEach { (category, params) ->
                item(key = category.name) {
                    CategoryHeader(category = category)
                }
                items(params, key = { it.name }) { param ->
                    ParameterRow(
                        param = param,
                        answer = assessment.answers[param],
                        onToggle = { viewModel.toggleAnswer(param) },
                        onSet = { value -> viewModel.setAnswer(param, value) },
                    )
                }
            }

            // Closing blessing
            item {
                Text(
                    "May all beings be happy… be peaceful… be liberated…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear today's assessment?") },
            text = { Text("All answers for ${state.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM"))} will be removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDay(); showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ──────────────────────────────────────────────
// Progress summary card
// ──────────────────────────────────────────────

@Composable
private fun AssessmentProgressCard(assessment: DayAssessment) {
    val answered = assessment.answeredCount()
    val total    = AssessmentParameter.entries.size
    val score    = assessment.score()
    val yesCount = assessment.answers.values.count { it == true }

    val scoreColor = when {
        score >= 0.8f -> Color(0xFF4CAF50)
        score >= 0.5f -> MaterialTheme.colorScheme.primary
        score >  0f   -> Color(0xFFFF9800)
        else          -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Circular score
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp),
            ) {
                CircularProgressIndicator(
                    progress = { if (answered > 0) score else 0f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    color = scoreColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Text(
                    text = if (answered > 0) "${(score * 100).toInt()}%" else "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        answered == 0    -> "Not started yet"
                        score >= 0.9f    -> "Excellent practice today 🌟"
                        score >= 0.7f    -> "Good progress today"
                        score >= 0.5f    -> "Halfway there"
                        answered < total -> "Keep going…"
                        else             -> "Assessment complete"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$answered of $total answered  •  $yesCount yes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Linear bar
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { answered.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Month heatmap
// ──────────────────────────────────────────────

@Composable
private fun MonthHeatmap(
    month: YearMonth,
    assessments: List<DayAssessment>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val scoreMap = assessments.associate { it.date to it.score() }
    val today    = LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev month", modifier = Modifier.size(20.dp))
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onNextMonth,
                    enabled = month < YearMonth.now(),
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Text(
                        d, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Days grid
            val firstDay = month.atDay(1)
            val startOffset = (firstDay.dayOfWeek.value - 1)  // Mon = 0
            val daysInMonth = month.lengthOfMonth()

            val cells = startOffset + daysInMonth
            val rows  = (cells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startOffset + 1
                        val date = if (dayNumber in 1..daysInMonth)
                            month.atDay(dayNumber) else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (date != null && date <= today) {
                                val score    = scoreMap[date]
                                val isSelected = date == selectedDate
                                val cellColor = when {
                                    score == null || score == 0f ->
                                        MaterialTheme.colorScheme.surfaceVariant
                                    score >= 0.8f ->
                                        Color(0xFF4CAF50).copy(alpha = 0.85f)
                                    score >= 0.5f ->
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else ->
                                        Color(0xFFFF9800).copy(alpha = 0.6f)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            ) else Modifier
                                        )
                                        .clickable { onSelectDate(date) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        dayNumber.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (score != null && score > 0f) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            } else if (date != null) {
                                // Future date — greyed out, not clickable
                                Text(
                                    dayNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }
            }

            // Legend
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(
                    Color(0xFF4CAF50).copy(alpha = 0.85f) to "≥80%",
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) to "≥50%",
                    Color(0xFFFF9800).copy(alpha = 0.6f) to "<50%",
                    MaterialTheme.colorScheme.surfaceVariant to "None",
                ).forEach { (color, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Category header
// ──────────────────────────────────────────────

@Composable
private fun CategoryHeader(category: AssessmentCategory) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(category.emoji, fontSize = 16.sp)
        Text(
            category.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
    }
}

// ──────────────────────────────────────────────
// Individual parameter row
// ──────────────────────────────────────────────

@Composable
private fun ParameterRow(
    param: AssessmentParameter,
    answer: Boolean?,
    onToggle: () -> Unit,
    onSet: (Boolean?) -> Unit,
) {
    val containerColor = when (answer) {
        true  -> Color(0xFF4CAF50).copy(alpha = 0.12f)
        false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        null  -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = when (answer) {
        true  -> Color(0xFF4CAF50).copy(alpha = 0.5f)
        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        null  -> Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Parameter number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    param.number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    param.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    param.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            // Yes / No / Clear buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Yes
                AnswerChip(
                    label = "✓",
                    selected = answer == true,
                    selectedColor = Color(0xFF4CAF50),
                    onClick = { onSet(if (answer == true) null else true) },
                )
                // No
                AnswerChip(
                    label = "✗",
                    selected = answer == false,
                    selectedColor = MaterialTheme.colorScheme.error,
                    onClick = { onSet(if (answer == false) null else false) },
                )
            }
        }
    }
}

@Composable
private fun AnswerChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ──────────────────────────────────────────────
// Bottom bar
// ──────────────────────────────────────────────

@Composable
private fun AssessmentBottomBar(
    assessment: DayAssessment,
    onClear: () -> Unit,
    onMarkAll: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
            Button(
                onClick = onMarkAll,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Mark all yes")
            }
        }
    }
}
