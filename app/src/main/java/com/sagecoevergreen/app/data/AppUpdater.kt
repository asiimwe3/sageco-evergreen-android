package com.sagecoevergreen.app.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds the live update state for the whole app.
 * checked: null while checking, false = up to date, true = update required.
 */
object AppUpdater {
    var checked by mutableStateOf<Boolean?>(null)
    var latestVersionCode by mutableStateOf(0)
    var latestNotes by mutableStateOf<String?>(null)
    var apkUrl by mutableStateOf<String?>(null)

    /** Downloads the update APK via DownloadManager and opens the installer. */
    fun downloadAndInstall(context: Context, url: String, onStatus: (String) -> Unit) {
        onStatus("Starting download…")
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("SAGECO Evergreen Update")
                .setDescription("Downloading latest version…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    "sageco-evergreen-update.apk"
                )
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = dm.enqueue(request)

            // Watch for completion, then fire the install intent
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE &&
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        val uri = dm.getUriForDownloadedFile(downloadId)
                        if (uri != null) {
                            onStatus("Download complete — opening installer…")
                            val install = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(install)
                            } catch (_: Exception) {
                                onStatus("Open the notification to install the update.")
                            }
                        } else {
                            onStatus("Download failed — check your connection and try again.")
                        }
                    }
                }
            }
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            onStatus("Downloading update… watch your notifications.")
        } catch (e: Exception) {
            onStatus("Download failed: ${e.message}")
        }
    }
}
