package com.sagecoevergreen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.Property
import com.sagecoevergreen.app.ui.components.*
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onPropertyClick: (String) -> Unit,
    onSeeAllProperties: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var featured by remember { mutableStateOf<List<Property>>(emptyList()) }
    var recent by remember { mutableStateOf<List<Property>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val (feat, _) = ApiClient.getProperties(featured = true, limit = 6)
                val (rec, _) = ApiClient.getProperties(sort = "newest", limit = 4)
                featured = feat
                recent = rec
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OffWhite),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Hero header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(SagecoGreen)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xDD051410), Color(0x80051A10), Color(0xDD051410)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = White,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("SE", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SagecoGreen)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SAGECO", fontSize = 18.sp, fontWeight = FontWeight.Black, color = White)
                            Text("EVERGREEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SagecoGreenBright, letterSpacing = 2.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Find Your Dream Property in Uganda", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Land, Homes, Commercial & Eco Projects", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                }
            }
        }

        // Search bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OffWhite)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    modifier = Modifier.fillMaxWidth().clickable { onSeeAllProperties() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Gray400, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Search properties, location...", fontSize = 14.sp, color = Gray400)
                    }
                }
            }
        }

        // Quick categories
        item {
            val cats = listOf(
                Triple("All", "🏠", SagecoGreen),
                Triple("Residential", "🏡", Color(0xFFDC2626)),
                Triple("Commercial", "🏢", Color(0xFF2563EB)),
                Triple("Land", "📍", SagecoGreen),
                Triple("Plot", "📐", Color(0xFF2563EB)),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cats) { (label, icon, color) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = White,
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onNavigate("properties?category=$label") }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(icon, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Gray800)
                        }
                    }
                }
            }
        }

        // Quick actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Become a Broker", Icons.Default.PersonAdd, SagecoGreen, Modifier.weight(1f)) { onNavigate("brokers") }
                QuickAction("Agent / MLM", Icons.Default.Group, Gold, Modifier.weight(1f)) { onNavigate("agents") }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Book Viewing", Icons.Default.CalendarMonth, Color(0xFF2563EB), Modifier.weight(1f)) { onNavigate("properties") }
                QuickAction("Chat with Us", Icons.Default.Chat, Color(0xFF7C3AED), Modifier.weight(1f)) { onNavigate("chat") }
            }
        }

        if (loading) {
            item { LoadingIndicator() }
        } else if (error != null) {
            item { ErrorView(error!!) { load() } }
        } else {
            // Featured properties
            if (featured.isNotEmpty()) {
                item {
                    SectionHeader("Featured Properties", "See All") { onSeeAllProperties() }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featured) { prop ->
                            Box(modifier = Modifier.width(260.dp)) {
                                PropertyCard(prop) { onPropertyClick(prop.id) }
                            }
                        }
                    }
                }
            }

            // Recent listings
            if (recent.isNotEmpty()) {
                item {
                    SectionHeader("New Listings", "See All") { onSeeAllProperties() }
                }
                items(recent) { prop ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        PropertyCard(prop) { onPropertyClick(prop.id) }
                    }
                }
            }
        }

        // Contact strip
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = SagecoGreen
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact Us", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("0750 414 366", fontSize = 14.sp, color = White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("sagecoevergreen@gmail.com", fontSize = 14.sp, color = White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kyenjojo, Western Uganda", fontSize = 14.sp, color = White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = White,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Gray800, maxLines = 2)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800)
        TextButton(onClick = onAction) {
            Text(action, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SagecoGreen)
        }
    }
}
