package com.sagecoevergreen.app.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler. Catches any uncaught exception, writes the full
 * stack trace to a file, and tries to beam it to the developer's server
 * BEFORE the system kills the process. If the send fails, the file stays
 * and MainActivity retries it on the next launch.
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
            // Send synchronously with a short timeout. The process is about
            // to die - a background thread would be killed before finishing
            // its HTTP call, which is why the old fire-and-forget approach
            // lost reports. Blocking here briefly is the standard pattern
            // (ACRA/Crashlytics do the same); the crash dialog just appears
            // a few seconds later.
            try {
                File(appContext.filesDir, FILE_NAME).readText().let {
                    sendRemotely(appContext, it, blocking = true)
                }
            } catch (_: Exception) {
            }
            // Let the system handle it (show crash dialog / kill process)
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Sends the crash report to the developer's crash-report endpoint.
     *
     * blocking = true  -> used from the crash handler; completes (or times
     *                     out) before the process dies, so the report lands.
     * blocking = false -> used for the next-launch retry; runs on a
     *                     background thread since the app is alive.
     */
    fun sendRemotely(context: Context, report: String, blocking: Boolean = false) {
        val sender = Runnable { doSend(context, report) }
        if (blocking) {
            sender.run()
        } else {
            Thread(sender).start()
        }
    }

    private fun doSend(context: Context, report: String) {
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
            // Short timeouts: in the crash path every second counts, and a
            // slow network must not hang the dying process for 15s.
            conn.connectTimeout = 6000
            conn.readTimeout = 4000
            conn.requestMethod = "POST"
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
