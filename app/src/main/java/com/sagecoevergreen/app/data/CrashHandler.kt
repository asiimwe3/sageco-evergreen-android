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

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(appContext, thread, throwable)
            } catch (_: Exception) {
            }
            // Let the system handle it (show crash dialog / kill process)
            previous?.uncaughtException(thread, throwable)
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
