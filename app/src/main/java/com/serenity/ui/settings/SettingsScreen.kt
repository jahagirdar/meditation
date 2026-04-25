package com.serenity.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.ui.theme.AccentColours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Appearance ──
            SettingsSectionHeader("Appearance")

            SettingsDropdown(
                label = "Theme",
                options = listOf(
                    "system" to "System default",
                    "light"  to "Light",
                    "dark"   to "Dark",
                    "amoled" to "AMOLED black",
                ),
                selected = prefs.themeMode,
                onSelect = { viewModel.setTheme(it) },
            )

            Text("Accent colour", style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentColours.all.forEach { (key, colour) ->
                    val selected = prefs.accentColour == key
                    IconButton(
                        onClick = { viewModel.setAccent(key) },
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = colour,
                            border = if (selected) ButtonDefaults.outlinedButtonBorder else null,
                        ) {
                            if (selected) {
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Check, null,
                                        tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Timer ──
            SettingsSectionHeader("Timer")

            SettingsSwitch(
                label = "Show elapsed time",
                subtitle = "Display time elapsed instead of remaining",
                checked = prefs.showElapsedTime,
                onToggle = { viewModel.setShowElapsed(it) },
            )
            SettingsSwitch(
                label = "Breathing animation",
                subtitle = "Subtle expanding circle during meditation",
                checked = prefs.breathingAnimation,
                onToggle = { viewModel.setBreathingAnim(it) },
            )
            SettingsSlider(
                label = "Daily goal",
                value = prefs.dailyGoalMinutes,
                range = 5..120, step = 5, unit = "min",
                onSet = { viewModel.setDailyGoal(it) },
            )
            SettingsSlider(
                label = "Dim screen after",
                value = prefs.dimScreenAfterSec,
                range = 10..120, step = 10, unit = "sec",
                onSet = { viewModel.setDimScreen(it) },
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Wearable ──
            SettingsSectionHeader("Smartwatch")

            SettingsSwitch(
                label = "Stress nudge",
                subtitle = "Show a calming session suggestion when your watch detects elevated stress",
                checked = prefs.stressNudgeEnabled,
                onToggle = { viewModel.setStressNudge(it) },
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Reminders ──
            SettingsSectionHeader("Daily Reminders")

            listOf(
                Triple(0, "Reminder 1", prefs.reminder1Time),
                Triple(1, "Reminder 2", prefs.reminder2Time),
                Triple(2, "Reminder 3", prefs.reminder3Time),
            ).forEach { (index, label, current) ->
                ReminderRow(
                    label = label,
                    time  = current,
                    onSet   = { viewModel.setReminder(index, it) },
                    onClear = { viewModel.clearReminder(index) },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Section header ──

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        fontWeight = FontWeight.SemiBold)
}

// ── Toggle row ──

@Composable
private fun SettingsSwitch(
    label: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

// ── Dropdown ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = { onSelect(key); expanded = false },
                )
            }
        }
    }
}

// ── Slider ──

@Composable
private fun SettingsSlider(
    label: String, value: Int, range: IntRange, step: Int, unit: String, onSet: (Int) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("$value $unit", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onSet(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / step - 1,
        )
    }
}

// ── Reminder row ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    label: String, time: String?, onSet: (String) -> Unit, onClear: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                time ?: "Off",
                style = MaterialTheme.typography.bodySmall,
                color = if (time != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            TextButton(onClick = { showPicker = true }) {
                Text(if (time != null) "Change" else "Set")
            }
            if (time != null) {
                TextButton(onClick = onClear) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Set $label") },
            text  = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    onSet("%02d:%02d".format(h, m))
                    showPicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }
}
