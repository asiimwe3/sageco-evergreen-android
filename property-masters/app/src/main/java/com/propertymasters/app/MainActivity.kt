package com.propertymasters.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.propertymasters.app.data.repository.SupabaseRepository
import com.propertymasters.app.ui.theme.PropertyMastersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PropertyMastersTheme {
                PropertyMastersApp()
                UpdateChecker()
            }
        }
    }
}

/**
 * In-app auto-update: checks the website for a newer APK version on every
 * launch (https://sageco-evergreen-co.vercel.app/api/app-version) and offers
 * a one-tap download of the latest build.
 */
@Composable
fun UpdateChecker() {
    var update by remember { mutableStateOf<SupabaseRepository.AppUpdate?>(null) }

    LaunchedEffect(Unit) {
        update = SupabaseRepository.checkForUpdate(BuildConfig.VERSION_CODE)
    }

    update?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!info.forceUpdate) update = null },
            title = { Text("Update Available — v${info.versionName}") },
            text = { Text(info.notes) },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
                    } catch (_: Exception) { }
                    if (!info.forceUpdate) update = null
                }) { Text("Download Update") }
            },
            dismissButton = {
                if (!info.forceUpdate) {
                    TextButton(onClick = { update = null }) { Text("Later") }
                }
            }
        )
    }
}
