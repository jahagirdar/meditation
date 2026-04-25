package com.serenity.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenity.domain.model.*

// ──────────────────────────────────────────────
// Timer Config Bottom Sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerConfigSheet(
    preset: Preset,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (Preset) -> Unit,
    onSaveAsNew: (Preset) -> Unit,
) {
    var edited by remember(preset.id) { mutableStateOf(preset) }
    var nameInput by remember(preset.id) { mutableStateOf(preset.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Configure Session", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it; edited = edited.copy(name = it) },
                label = { Text("Preset name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ConfigSection("Duration") {
                DurationPicker(seconds = edited.durationSec, onPick = { edited = edited.copy(durationSec = it) })
            }
            ConfigSection("Warm-up") {
                WarmCoolPicker("Warm-up before first bell", edited.warmupSec) { edited = edited.copy(warmupSec = it) }
            }
            ConfigSection("Cool-down") {
                WarmCoolPicker("Cool-down after session", edited.cooldownSec) { edited = edited.copy(cooldownSec = it) }
            }
            ConfigSection("Interval bells") {
                IntervalPicker(selected = edited.intervalOption) { edited = edited.copy(intervalOption = it) }
            }
            ConfigSection("Bell sounds") {
                BellSoundPicker("Start bell",    edited.startBell)    { edited = edited.copy(startBell = it) }
                Spacer(Modifier.height(8.dp))
                BellSoundPicker("Interval bell", edited.intervalBell) { edited = edited.copy(intervalBell = it) }
                Spacer(Modifier.height(8.dp))
                BellSoundPicker("End bell",      edited.endBell)      { edited = edited.copy(endBell = it) }
            }
            ConfigSection("Ambient sound") {
                AmbientSoundPicker(selected = edited.ambientSound) { edited = edited.copy(ambientSound = it) }
            }
            ConfigSection("") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Silent mode", style = MaterialTheme.typography.bodyLarge)
                        Text("Haptic feedback only, no sounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = edited.silentMode,
                        onCheckedChange = { edited = edited.copy(silentMode = it) })
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onSaveAsNew(edited) }, modifier = Modifier.weight(1f)) {
                    Text("Save as new")
                }
                Button(onClick = { onSave(edited) }, modifier = Modifier.weight(1f)) {
                    Text("Apply")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConfigSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotBlank()) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

// ──────────────────────────────────────────────
// Duration Picker
// ──────────────────────────────────────────────

@Composable
fun DurationPicker(seconds: Int, onPick: (Int) -> Unit) {
    val quickOptions = listOf(5, 10, 15, 20, 30, 45, 60)
    val mins = seconds / 60
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = { if (mins > 1) onPick((mins - 1) * 60) }) {
            Icon(Icons.Default.Remove, "Decrease")
        }
        Text("${mins}m", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light, modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onPick((mins + 1) * 60) }) {
            Icon(Icons.Default.Add, "Increase")
        }
    }
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(quickOptions) { m ->
            FilterChip(selected = seconds == m * 60, onClick = { onPick(m * 60) },
                label = { Text("${m}m") })
        }
    }
}

// ──────────────────────────────────────────────
// Warm-up / Cool-down Picker
// ──────────────────────────────────────────────

@Composable
fun WarmCoolPicker(label: String, seconds: Int, onPick: (Int) -> Unit) {
    val options = listOf(0, 30, 60, 90, 120, 180, 300)
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options) { s ->
                FilterChip(selected = seconds == s, onClick = { onPick(s) },
                    label = { Text(if (s == 0) "Off" else if (s < 60) "${s}s" else "${s/60}m") })
            }
        }
    }
}

// ──────────────────────────────────────────────
// Interval Picker
// ──────────────────────────────────────────────

@Composable
fun IntervalPicker(selected: IntervalOption?, onPick: (IntervalOption?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = selected == null, onClick = { onPick(null) },
                label = { Text("Off") })
        }
        items(IntervalOption.entries) { option ->
            FilterChip(selected = selected == option, onClick = { onPick(option) },
                label = { Text(option.displayName) })
        }
    }
}

// ──────────────────────────────────────────────
// Bell Sound Picker
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellSoundPicker(label: String, selected: BellSound, onPick: (BellSound) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BellSound.entries.forEach { bell ->
                DropdownMenuItem(
                    text = { Text(bell.displayName) },
                    onClick = { onPick(bell); expanded = false },
                    leadingIcon = {
                        if (selected == bell) Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Ambient Sound Picker
// ──────────────────────────────────────────────

@Composable
fun AmbientSoundPicker(selected: AmbientSound, onPick: (AmbientSound) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AmbientSound.entries) { sound ->
            FilterChip(selected = selected == sound, onClick = { onPick(sound) },
                label = { Text(sound.displayName) })
        }
    }
}

// ──────────────────────────────────────────────
// Preset Card
// ──────────────────────────────────────────────

@Composable
fun PresetCard(
    preset: Preset,
    isActive: Boolean,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val durationMin = preset.durationSec / 60
    Card(
        modifier = Modifier.width(130.dp).height(140.dp).clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(preset.name, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text("${durationMin}m", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    if (preset.intervalOption != null)
                        Text("⏱ ${preset.intervalOption.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (preset.warmupSec > 0)
                        Text("↑ ${preset.warmupSec/60}m warmup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Start now") },
                        onClick = { onStart(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}
