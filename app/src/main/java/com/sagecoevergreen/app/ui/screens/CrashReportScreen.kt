package com.sagecoevergreen.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.data.CrashHandler
import com.sagecoevergreen.app.ui.theme.*

/**
 * Shown on launch when the previous session crashed.
 * Displays the full stack trace so it can be reported to the developer.
 */
@Composable
fun CrashReportScreen(report: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
            .padding(16.dp)
    ) {
        Text(
            "⚠️ The app closed unexpectedly",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "A crash report was saved. Please screenshot this screen or copy the report and send it to DeryCode on WhatsApp so it can be fixed.",
            fontSize = 13.sp,
            color = Gray600
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Crash Report", report))
                    copied = true
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen)
            ) {
                Text(if (copied) "✓ Copied" else "Copy report")
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = {
                    CrashHandler.clear(context)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Continue to app")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = White,
            tonalElevation = 1.dp
        ) {
            Text(
                report,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Gray800,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
