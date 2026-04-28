package com.serenity.crash

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG        = "SerenityBugReport"
    private const val MAX_LOGS   = 5          // keep last 5 crash files
    private lateinit var appContext: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        appContext       = context.applicationContext
        defaultHandler   = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildReport(thread, throwable)
            writeToFile(report)
            Log.e(TAG, report)
        } catch (e: Exception) {
            Log.e(TAG, "CrashHandler itself threw", e)
        } finally {
            // Let the system show its usual "app stopped" dialog
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    // ── Report building ───────────────────────────────────────────────────

    private fun buildReport(thread: Thread, t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)

        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        pw.println("=== Serenity Crash Report ===")
        pw.println("Time    : $ts")
        pw.println("Thread  : ${thread.name} (id=${thread.id})")
        pw.println("Process : ${android.os.Process.myPid()}")
        pw.println()

        // Android version + device info
        pw.println("--- Device ---")
        pw.println("Android : ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        pw.println("Device  : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        pw.println()

        // App version
        try {
            val pkg = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            pw.println("--- App ---")
            pw.println("Version : ${pkg.versionName} (${pkg.longVersionCode})")
            pw.println()
        } catch (_: Exception) {}

        // Full stack trace (cause chain)
        pw.println("--- Stack Trace ---")
        t.printStackTrace(pw)
        var cause = t.cause
        while (cause != null) {
            pw.println("\nCaused by:")
            cause.printStackTrace(pw)
            cause = cause.cause
        }

        pw.flush()
        return sw.toString()
    }

    // ── File I/O ──────────────────────────────────────────────────────────

    private fun writeToFile(report: String) {
        val dir = crashDir() ?: return
        val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        File(dir, "crash_$ts.txt").writeText(report, Charsets.UTF_8)
        pruneOldLogs(dir)
    }

    private fun pruneOldLogs(dir: File) {
        dir.listFiles()
            ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_LOGS)
            ?.forEach { it.delete() }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Returns all saved crash report files, newest first. */
    fun savedReports(context: Context): List<File> =
        crashDir(context)
            ?.listFiles()
            ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** True if at least one crash report exists that hasn't been acknowledged. */
    fun hasUnreadReport(context: Context): Boolean =
        savedReports(context).isNotEmpty()

    fun clearAll(context: Context) {
        crashDir(context)?.listFiles()?.forEach { it.delete() }
    }

    private fun crashDir(context: Context = appContext): File? =
        try {
            File(context.filesDir, "crashes").also { it.mkdirs() }
        } catch (_: Exception) { null }
}
