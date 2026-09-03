package com.sagecoevergreen.app.ui.screens

import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.AppVersion
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    savedAgentId: String?,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf<AppVersion?>(null) }

    LaunchedEffect(Unit) {
        scope.launch { version = ApiClient.checkVersion() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(OffWhite).verticalScroll(rememberScrollState())
    ) {
        // Profile header
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = CircleShape, color = White.copy(alpha = 0.2f), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Person, null, tint = White, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("SAGECO Evergreen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                Text("User Account", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                if (savedAgentId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = White.copy(alpha = 0.2f)) {
                        Text(
                            "Agent ID: ${savedAgentId.take(8)}...",
                            fontSize = 11.sp, color = White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu items
        AccountMenuItem(
            icon = Icons.Default.Home,
            title = "My Properties",
            subtitle = "Browse all listings"
        ) { onNavigate("properties") }

        AccountMenuItem(
            icon = Icons.Default.PersonAdd,
            title = "Become a Broker",
            subtitle = "Register as a verified broker"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sageco-evergreen-co.vercel.app/broker-register"))
            context.startActivity(intent)
        }

        AccountMenuItem(
            icon = Icons.Default.Group,
            title = "Agent / MLM Network",
            subtitle = "Earnings, downline & withdrawals"
        ) { onNavigate("agents") }

        AccountMenuItem(
            icon = Icons.Default.CalendarMonth,
            title = "Book a Viewing",
            subtitle = "Schedule property site visits"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sageco-evergreen-co.vercel.app/book"))
            context.startActivity(intent)
        }

        AccountMenuItem(
            icon = Icons.Default.Upload,
            title = "Upload Property",
            subtitle = "List your property for sale"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sageco-evergreen-co.vercel.app/upload-property"))
            context.startActivity(intent)
        }

        AccountMenuItem(
            icon = Icons.Default.AttachMoney,
            title = "Subscription Plans",
            subtitle = "Upgrade your broker plan"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sageco-evergreen-co.vercel.app/plans"))
            context.startActivity(intent)
        }

        AccountMenuItem(
            icon = Icons.Default.Chat,
            title = "Help & Support",
            subtitle = "Chat with our assistant"
        ) { onNavigate("chat") }

        AccountMenuItem(
            icon = Icons.Default.Phone,
            title = "Contact Us",
            subtitle = "0750 414 366  •  WhatsApp"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/256750414366"))
            context.startActivity(intent)
        }

        AccountMenuItem(
            icon = Icons.Default.Shield,
            title = "Privacy Policy",
            subtitle = "How we handle your data"
        ) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sageco-evergreen-co.vercel.app/privacy"))
            context.startActivity(intent)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About section
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            color = White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800)
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAGECO EVERGREEN CO. LTD", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Text("Kyenjojo, Western Uganda", fontSize = 13.sp, color = Gray600)
                Text("Mon–Sat, 8:00 AM – 6:00 PM EAT", fontSize = 13.sp, color = Gray600)
                Spacer(modifier = Modifier.height(8.dp))
                version?.let {
                    Text("App Version: ${it.versionName} (${it.versionCode})", fontSize = 12.sp, color = Gray400)
                } ?: Text("App Version: 4.0.0 (10)", fontSize = 12.sp, color = Gray400)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = SagecoGreen, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Text(subtitle, fontSize = 12.sp, color = Gray400)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Gray400, modifier = Modifier.size(20.dp))
        }
    }
}
