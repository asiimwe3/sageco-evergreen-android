package com.sagecoevergreen.app.data

import android.content.Context
import java.io.File

/**
 * Builds a full diagnostic report the user can share to the developer via
 * WhatsApp with one tap (Account screen -> "Send Error Logs").
 *
 * Includes the last saved crash (if any) plus a logcat dump of this app's
 * own process - which is allowed without any special permissions and often
 * captures startup errors that never reach the crash handler (ANRs, native
 * crashes, system kills).
 */
object DiagnosticsCollector {

    private const val MAX_LOGCAT_CHARS = 60_000

    fun buildReport(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("SAGECO Evergreen diagnostics")
        sb.appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine()
        sb.appendLine("── Device ──")
        sb.appendLine("Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("App version: ${pi.versionName} (build ${if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()})")
        } catch (_: Exception) {
            sb.appendLine("App version: unknown")
        }
        sb.appendLine()

        sb.appendLine("── Last crash (if the app crashed since launch) ──")
        val lastCrash = CrashHandler.getLastCrash(context)
        sb.appendLine(lastCrash ?: "(no saved crash report — the app did not record a crash since the last clean launch)")
        sb.appendLine()

        sb.appendLine("── Recent app logs (logcat, this process) ──")
        sb.appendLine(getLogcat())
        return sb.toString()
    }

    /** Dumps this process's own logcat. Returns an empty note if denied. */
    private fun getLogcat(): String {
        return try {
            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "--pid=$pid"))
            val output = process.inputStream.bufferedReader().readText()
            if (output.isBlank()) "(logcat returned nothing for this process)" else output.take(MAX_LOGCAT_CHARS)
        } catch (e: Exception) {
            "(logcat unavailable: ${e.message})"
        }
    }

    /**
     * Writes the report to a file WhatsApp can share as a document and opens
     * the WhatsApp share sheet. Falls back to a plain text share if WhatsApp
     * is unavailable.
     */
    fun shareToWhatsApp(context: Context) {
        val report = buildReport(context)
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val file = File(dir, "sageco-diagnostics-${System.currentTimeMillis()}.txt")
        file.writeText(report)

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Prefer WhatsApp; if missing, let the user pick any app
            val wa = android.content.Intent(intent).setPackage("com.whatsapp")
            val receiver = if (context.packageManager.resolveActivity(wa, 0) != null) wa else intent
            context.startActivity(
                android.content.Intent.createChooser(receiver, "Send error logs to the developer")
            )
        } catch (_: Exception) {
            // Last resort: share as plain text
            try {
                val text = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, report.take(40_000))
                }
                context.startActivity(android.content.Intent.createChooser(text, "Send error logs to the developer"))
            } catch (_: Exception) {
            }
        }
    }
}
