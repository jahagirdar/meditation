package com.serenity.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    onBack: () -> Unit,
    viewModel: AudioSettingsViewModel = hiltViewModel(),
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ── Permission launcher ──
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onAudioPermissionGranted()
    }

    // ── Audio file picker (content URI) ──
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.setCustomAmbientUri(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Missing files banner ─────────────────────────────────────
            if (state.missingBells.isNotEmpty() || state.missingPranayamaCues.isNotEmpty()) {
                MissingFilesBanner(
                    missingBells        = state.missingBells,
                    missingPranayama    = state.missingPranayamaCues,
                    useFallback         = state.useFallbackForMissing,
                    onToggleFallback    = { viewModel.toggleFallback() },
                )
            } else {
                AllFilesOkBanner()
            }

            Divider()

            // ── Ambient sound source ─────────────────────────────────────
            Text(
                "Ambient sound source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Choose whether ambient sounds come from the built-in library or a custom audio file from your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Built-in vs Custom radio
            AmbientSourceSelector(
                useCustom = state.useCustomAmbient,
                onSelectBuiltIn = { viewModel.setUseCustomAmbient(false) },
                onSelectCustom  = {
                    viewModel.setUseCustomAmbient(true)
                    // Request audio permission if needed, then open picker
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_AUDIO
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    val granted = ContextCompat.checkSelfPermission(context, perm) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) filePicker.launch(arrayOf("audio/*"))
                    else permissionLauncher.launch(perm)
                },
            )

            // Custom file info
            AnimatedVisibility(visible = state.useCustomAmbient) {
                CustomAmbientCard(
                    uri         = state.customAmbientUri,
                    displayName = state.customAmbientName,
                    onPick      = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_AUDIO
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        val granted = ContextCompat.checkSelfPermission(context, perm) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) filePicker.launch(arrayOf("audio/*"))
                        else permissionLauncher.launch(perm)
                    },
                    onClear     = { viewModel.clearCustomAmbient() },
                )
            }

            // Permission granted callback — open picker
            LaunchedEffect(state.permissionJustGranted) {
                if (state.permissionJustGranted) {
                    filePicker.launch(arrayOf("audio/*"))
                    viewModel.consumePermissionGranted()
                }
            }

            Divider()

            // ── What to do about missing files ───────────────────────────
            Text(
                "Adding your own bell sounds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            MissingFilesGuide()
        }
    }
}

// ──────────────────────────────────────────────
// Missing files banner
// ──────────────────────────────────────────────

@Composable
private fun MissingFilesBanner(
    missingBells: List<String>,
    missingPranayama: List<String>,
    useFallback: Boolean,
    onToggleFallback: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp))
                Text(
                    "${missingBells.size + missingPranayama.size} audio file(s) missing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (missingBells.isNotEmpty()) {
                Text("Bell sounds missing:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                missingBells.forEach { name ->
                    Text("  • $name.mp3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (missingPranayama.isNotEmpty()) {
                Text("Pranayama cues missing:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                missingPranayama.forEach { name ->
                    Text("  • $name.mp3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

            // Fallback toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use notification sound as fallback",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text(
                        if (useFallback)
                            "Missing bells will play your default notification sound"
                        else
                            "Missing bells will be silent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = useFallback, onCheckedChange = { onToggleFallback() })
            }
        }
    }
}

@Composable
private fun AllFilesOkBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CheckCircle, null,
                tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
            Column {
                Text("All audio files present",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50))
                Text("Bells and ambient sounds are ready to play",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────────────────────────
// Ambient source selector
// ──────────────────────────────────────────────

@Composable
private fun AmbientSourceSelector(
    useCustom: Boolean,
    onSelectBuiltIn: () -> Unit,
    onSelectCustom: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = !useCustom,
            onClick  = onSelectBuiltIn,
            label    = { Text("Built-in library") },
            leadingIcon = {
                if (!useCustom) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = useCustom,
            onClick  = onSelectCustom,
            label    = { Text("From my phone") },
            leadingIcon = {
                if (useCustom) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                else Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

// ──────────────────────────────────────────────
// Custom ambient card
// ──────────────────────────────────────────────

@Composable
private fun CustomAmbientCard(
    uri: Uri?,
    displayName: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (uri != null) Icons.Default.AudioFile else Icons.Default.FolderOpen,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName ?: "No file selected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Text(
                    if (uri != null) "This file will loop as ambient sound during sessions"
                    else "Tap 'Browse' to pick an audio file from your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(if (uri != null) "Change" else "Browse")
                }
                if (uri != null) {
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Guide card
// ──────────────────────────────────────────────

@Composable
private fun MissingFilesGuide() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Place .mp3 files in:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(
                "app/src/main/res/raw/",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Text("Free sources:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("freesound.org", "mixkit.co/free-sound-effects", "zapsplat.com").forEach {
                Text("  • $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
