package com.sagecoevergreen.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.BuildConfig
import com.sagecoevergreen.app.data.AppUpdater
import com.sagecoevergreen.app.ui.theme.*

/**
 * Blocking update screen. Shown whenever the installed build is older than
 * the latest release served by the app-version API. The user cannot use a
 * stale build — this guarantees everyone runs the latest fixes.
 */
@Composable
fun UpdateScreen(
    installedCode: Int,
    latestCode: Int,
    notes: String?,
    apkUrl: String?
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(SagecoGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Update Required",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "A new version of SAGECO Evergreen is available.\nYou're on build $installedCode — the latest is build $latestCode.",
                fontSize = 14.sp,
                color = Color(0xFFCFE8DA),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This update fixes data and image loading issues.",
                fontSize = 13.sp,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            notes?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = Gray400, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    if (apkUrl != null) {
                        downloading = true
                        AppUpdater.downloadAndInstall(context, apkUrl) { s -> status = s }
                    }
                },
                enabled = apkUrl != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (downloading) "Downloading…" else "Download & Install Update",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
            status?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, fontSize = 12.sp, color = White, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                apkUrl?.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
            }) {
                Text("Or download in browser", fontSize = 13.sp, color = Color(0xFFCFE8DA))
            }
        }
    }
}
