package com.serenity.crash

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs `logcat` in a background thread and saves its output alongside crash reports.
 * Useful for capturing Hilt/Compose errors that don't always throw uncaught exceptions
 * (e.g. Compose snapshot assertion failures, Hilt missing binding errors).
 *
 * Call [start] early in Application.onCreate and [flush] when the app goes to background
 * or just before reading the crash report.
 */
object LogCollector {

    private const val TAG      = "SerenityLogCollector"
    private const val MAX_LINES = 2000

    @Volatile private var process: Process? = null
    private lateinit var appContext: Context

    fun start(context: Context) {
        appContext = context.applicationContext
        try {
            // Clear old logcat buffer so we only get logs from this session
            Runtime.getRuntime().exec(arrayOf("logcat", "-c"))

            process = Runtime.getRuntime().exec(
                arrayOf(
                    "logcat",
                    "-v", "threadtime",          // timestamp + thread id
                    "*:W",                       // Warnings and above from all tags
                    "SerenityBugReport:V",       // Verbose from our own crash handler
                    "AndroidRuntime:V",          // JVM crash output
                    "System.err:V",              // printStackTrace to stderr
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not start logcat collector", e)
        }
    }

    /**
     * Read what logcat has collected so far and write to a file.
     * Safe to call from any thread.
     */
    fun flush() {
        val proc = process ?: return
        try {
            val lines = proc.inputStream.bufferedReader()
                .readLines()
                .takeLast(MAX_LINES)

            if (lines.isEmpty()) return

            val dir = crashDir() ?: return
            val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "logcat_$ts.txt")
            file.writeText(lines.joinToString("\n"), Charsets.UTF_8)

            // Prune old logcat files (keep last 3)
            dir.listFiles()
                ?.filter { it.name.startsWith("logcat_") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(3)
                ?.forEach { it.delete() }

        } catch (e: Exception) {
            Log.e(TAG, "Flush failed", e)
        }
    }

    fun savedLogcats(context: Context): List<File> =
        crashDir(context)
            ?.listFiles()
            ?.filter { it.name.startsWith("logcat_") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun crashDir(context: Context = appContext): File? =
        try { File(context.filesDir, "crashes").also { it.mkdirs() } }
        catch (_: Exception) { null }
}
