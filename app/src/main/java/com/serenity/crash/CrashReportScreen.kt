package com.serenity.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenity.crash.LogCollector
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Flush logcat buffer when the screen is opened so we capture
    // any warnings that led up to the crash
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LogCollector.flush()
        }
    }

    var reports by remember {
        // Merge crash reports and logcat snapshots into one list, newest first
        val crashes  = CrashHandler.savedReports(context)
        val logcats  = LogCollector.savedLogcats(context)
        mutableStateOf((crashes + logcats).sortedByDescending { it.lastModified() })
    }
    var selectedReport by remember { mutableStateOf<File?>(reports.firstOrNull()) }
    var selectedText   by remember { mutableStateOf(selectedReport?.readText() ?: "") }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crash reports", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${reports.size} report(s) saved",
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
                    if (reports.isNotEmpty()) {
                        // Copy current report to clipboard
                        IconButton(onClick = {
                            copyToClipboard(context, selectedText)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                        // Clear all
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "Clear all",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (reports.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("✅", fontSize = 48.sp)
                    Text("No crash reports", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The app hasn't recorded any crashes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Report selector tabs ──────────────────────────────────────
            if (reports.size > 1) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(reports) { file ->
                        val prefix = when {
                    file.name.startsWith("crash_")  -> "💥 "
                    file.name.startsWith("logcat_") -> "📋 "
                    else -> ""
                }
                val label = prefix + parseDateLabel(file.name)
                        FilterChip(
                            selected  = file == selectedReport,
                            onClick   = {
                                selectedReport = file
                                selectedText   = file.readText()
                            },
                            label     = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // ── Crash summary card ────────────────────────────────────────
            selectedText.lines().take(12).let { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        summary.forEach { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize   = 11.sp,
                                    ),
                                    color = if (line.contains("Exception") || line.contains("Error"))
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // ── Sharing instructions ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Text(
                        "Tap the copy icon ↗ and paste the report into a GitHub issue or message.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Full log scrollable view ──────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        selectedText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 10.sp,
                            lineHeight  = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    // ── Clear confirmation ────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all crash reports?") },
            text  = { Text("${reports.size} report(s) will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    CrashHandler.clearAll(context)
                    LogCollector.savedLogcats(context).forEach { it.delete() }
                    reports        = emptyList()
                    selectedReport = null
                    selectedText   = ""
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, text: String) {
    val cm   = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Serenity crash report", text)
    cm.setPrimaryClip(clip)
}

/** "crash_20240428_143022.txt" → "28 Apr 14:30" */
private fun parseDateLabel(filename: String): String {
    return try {
        val raw  = filename.removePrefix("crash_").removeSuffix(".txt")
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(raw)
        SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(date!!)
    } catch (_: Exception) {
        filename
    }
}
