package com.sagecoevergreen.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.sagecoevergreen.app.data.Property
import com.sagecoevergreen.app.ui.theme.*

fun formatPrice(price: Long): String {
    return when {
        price >= 1_000_000 -> "UGX ${(price / 1_000_000.0).let { if (it % 1 == 0.0) it.toInt().toString() + "M" else String.format("%.1fM", it) }}"
        price >= 1_000 -> "UGX ${price / 1_000}K"
        else -> "UGX $price"
    }
}

@Composable
fun PropertyImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp)
) {
    val context = LocalContext.current
    if (url != null && url.startsWith("http")) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Gray100),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SagecoGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Gray200),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, null, tint = Gray400, modifier = Modifier.size(40.dp))
                }
            }
        )
    } else {
        Box(
            modifier = modifier.background(Gray200),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Home, null, tint = Gray400, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun PropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            PropertyImage(
                url = property.images?.firstOrNull(),
                contentDescription = property.title,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPrice(property.price),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SagecoGreen
                    )
                    if (property.featured) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldLight
                        ) {
                            Text(
                                "Featured",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = property.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Gray400, modifier = Modifier.size(14.dp))
                    Text(
                        text = property.location ?: "Uganda",
                        fontSize = 12.sp,
                        color = Gray600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val details = mutableListOf<String>()
                property.bedrooms?.let { details.add("$it bed") }
                property.bathrooms?.let { details.add("$it bath") }
                property.land_acres?.let { details.add("${it} acres") }
                property.area_sqft?.let { details.add("${it.toInt()} sqft") }
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = details.joinToString("  •  "),
                        fontSize = 11.sp,
                        color = Gray400
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) SagecoGreen else Gray100,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) White else Gray600,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = SagecoGreen)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudOff, null, tint = Gray400, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, fontSize = 14.sp, color = Gray600)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen)) {
            Text("Retry")
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, null, tint = Gray400, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, fontSize = 14.sp, color = Gray600)
    }
}
