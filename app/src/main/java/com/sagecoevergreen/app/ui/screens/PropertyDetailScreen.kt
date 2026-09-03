package com.sagecoevergreen.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun PropertyDetailScreen(
    propertyId: String,
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var property by remember { mutableStateOf<Property?>(null) }
    var similar by remember { mutableStateOf<List<Property>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentImageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(propertyId) {
        scope.launch {
            loading = true
            error = null
            try {
                val prop = ApiClient.getProperty(propertyId)
                property = prop
                if (prop != null && prop.category != null) {
                    val (sim, _) = ApiClient.getProperties(category = prop.category, limit = 4)
                    similar = sim.filter { it.id != propertyId }
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            }
            loading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().background(OffWhite), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SagecoGreen)
        }
        return
    }

    if (error != null || property == null) {
        Column(modifier = Modifier.fillMaxSize().background(OffWhite), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ErrorView(error ?: "Property not found") { onBack() }
        }
        return
    }

    val prop = property!!
    val images = prop.images ?: emptyList()

    LazyColumn(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Image gallery
        item {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Gray200)) {
                if (images.isNotEmpty()) {
                    AsyncImage(
                        model = images[currentImageIndex.coerceIn(0, images.lastIndex)],
                        contentDescription = prop.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Back button
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(36.dp)
                        .clickable { onBack() }
                        .align(Alignment.TopStart)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.ArrowBack, null, tint = White, modifier = Modifier.size(20.dp))
                    }
                }
                // Image counter
                if (images.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x99000000),
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            "${currentImageIndex + 1}/${images.size}",
                            color = White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                // Image dots
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        images.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentImageIndex) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i == currentImageIndex) White else Color(0x80FFFFFF))
                            )
                        }
                    }
                }
            }
        }

        // Thumbnail strip
        if (images.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { img ->
                        AsyncImage(
                            model = img,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentImageIndex = images.indexOf(img) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Title & price
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(prop.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gray800)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatPrice(prop.price), fontSize = 24.sp, fontWeight = FontWeight.Black, color = SagecoGreen)
                    if (prop.is_negotiable == true) {
                        Surface(shape = RoundedCornerShape(6.dp), color = GreenBg) {
                            Text("Negotiable", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SagecoGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Gray400, modifier = Modifier.size(16.dp))
                    Text(prop.location ?: "Uganda", fontSize = 14.sp, color = Gray600)
                }
            }
        }

        // Key details
        item {
            val details = mutableListOf<Pair<String, String>>()
            prop.bedrooms?.let { details.add("Bedrooms" to it.toString()) }
            prop.bathrooms?.let { details.add("Bathrooms" to it.toString()) }
            prop.area_sqft?.let { details.add("Area" to "${it.toInt()} sqft") }
            prop.land_acres?.let { details.add("Land" to "$it acres") }
            prop.plot_feet?.let { details.add("Plot" to it) }
            prop.category?.let { details.add("Type" to it) }
            prop.tenure_type?.let { details.add("Tenure" to it) }
            prop.title_deed?.let { details.add("Title Deed" to it) }
            prop.road_distance_km?.let { details.add("Road" to "$it km") }

            if (details.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Property Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800)
                        Spacer(modifier = Modifier.height(12.dp))
                        details.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                row.forEach { (label, value) ->
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontSize = 11.sp, color = Gray400)
                                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        // Utilities
                        val utils = mutableListOf<String>()
                        if (prop.water_available == true) utils.add("Water Available")
                        if (prop.electricity_available == true) utils.add("Electricity")
                        prop.fence?.let { utils.add("Fence: $it") }
                        if (utils.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            utils.forEach { u ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SagecoGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(u, fontSize = 13.sp, color = Gray800)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Description
        prop.description?.let { desc ->
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(desc, fontSize = 14.sp, color = Gray600, lineHeight = 22.sp)
                }
            }
        }

        // Contact card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = SagecoGreen
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact Agent", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (prop.broker_name != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prop.broker_name, fontSize = 14.sp, color = White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (prop.contact_phone != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prop.contact_phone, fontSize = 14.sp, color = White)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (prop.contact_phone != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prop.contact_phone}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = {
                                val phone = prop.contact_phone?.replace(" ", "") ?: ""
                                val msg = "I'm interested in: ${prop.title} (${formatPrice(prop.price)})"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/256${phone.trimStart('0')}?text=${Uri.encode(msg)}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, null, tint = SagecoGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 13.sp, color = SagecoGreen)
                        }
                    }
                }
            }
        }

        // Similar properties
        if (similar.isNotEmpty()) {
            item {
                Text("Similar Properties", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800, modifier = Modifier.padding(16.dp))
            }
            items(similar) { sim ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PropertyCard(sim) { onPropertyClick(sim.id) }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
