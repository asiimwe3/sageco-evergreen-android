package com.sagecoevergreen.app.ui.screens

import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.Broker
import com.sagecoevergreen.app.ui.components.*
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BrokersScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var brokers by remember { mutableStateOf<List<Broker>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                brokers = ApiClient.getBrokers(search.ifBlank { null })
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Header
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Brokers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Verified real estate professionals", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = White) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Search brokers...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { search = searchInput.trim(); load() }) {
                    Text("Search", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loading) {
            LoadingIndicator()
        } else if (error != null) {
            ErrorView(error!!) { load() }
        } else if (brokers.isEmpty()) {
            EmptyState("No brokers found")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(brokers) { broker ->
                    BrokerCard(broker) {
                        val phone = broker.phone?.replace(" ", "") ?: ""
                        if (phone.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokerCard(broker: Broker, onCall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            if (broker.photo_url != null && broker.photo_url.startsWith("http")) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(broker.photo_url)
                        .crossfade(true)
                        .build(),
                    contentDescription = broker.full_name,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize().background(SagecoGreen.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = SagecoGreen)
                        }
                    },
                    error = {
                        Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(broker.full_name.take(2).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                            }
                        }
                    }
                )
            } else {
                Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            broker.full_name.take(2).uppercase(),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SagecoGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        broker.full_name,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gray800,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (broker.verified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = SagecoGreen, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                broker.specialization?.let {
                    Text(it, fontSize = 12.sp, color = Gray600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Gray400, modifier = Modifier.size(12.dp))
                    Text(broker.location ?: "Uganda", fontSize = 11.sp, color = Gray400)
                }
            }

            // Call button
            if (broker.phone != null) {
                Surface(
                    shape = CircleShape,
                    color = SagecoGreen,
                    modifier = Modifier.size(40.dp).clickable(onClick = onCall)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Phone, null, tint = White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
