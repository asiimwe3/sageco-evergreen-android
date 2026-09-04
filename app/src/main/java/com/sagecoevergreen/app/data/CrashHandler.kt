package com.sagecoevergreen.app.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler. Catches any uncaught exception, writes the full
 * stack trace to a file, so on next launch the app can show exactly
 * what went wrong instead of silently closing.
 */
object CrashHandler {

    private const val FILE_NAME = "last_crash.txt"
    private const val CRASH_ENDPOINT = "https://sageco-evergreen-co.vercel.app/api/crash-report"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(appContext, thread, throwable)
            } catch (_: Exception) {
            }
            // Try to beam the report to the developer's server right away
            try {
                File(appContext.filesDir, FILE_NAME).readText().let {
                    sendRemotely(appContext, it)
                }
            } catch (_: Exception) {
            }
            // Let the system handle it (show crash dialog / kill process)
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Sends the crash report to the developer's crash-report endpoint (fire-and-forget). */
    fun sendRemotely(context: Context, report: String) {
        Thread {
            try {
                val appVersion = try {
                    val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                    "${pi.versionName} (${if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()})"
                } catch (_: Exception) { "unknown" }
                val payload = org.json.JSONObject().apply {
                    put("report", report)
                    put("version", appVersion)
                    put("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    put("androidVersion", android.os.Build.VERSION.RELEASE ?: "unknown")
                }
                val conn = java.net.URL(CRASH_ENDPOINT).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "SagecoApp/4.0 Android")
                try {
                    conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        // Sent successfully — clear the local file so we don't resend
                        clear(context)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Keep the local file; will retry on next launch
            }
        }.start()
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            appendLine("SAGECO Evergreen crash report")
            appendLine("Time: $stamp")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine(throwable.stackTraceToString())
            appendLine()
            appendLine("Cause chain:")
            var c = throwable.cause
            while (c != null) {
                appendLine(c.stackTraceToString())
                c = c.cause
            }
        }
        File(context.filesDir, FILE_NAME).writeText(text)
    }

    /** Returns the saved crash report, or null if the last session was clean. */
    fun getLastCrash(context: Context): String? {
        return try {
            val f = File(context.filesDir, FILE_NAME)
            if (f.exists()) f.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    /** Call after the user has seen/sent the report. */
    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
        }
    }
}
