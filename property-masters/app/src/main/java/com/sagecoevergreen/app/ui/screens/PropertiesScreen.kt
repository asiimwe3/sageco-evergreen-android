package com.sagecoevergreen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.Property
import com.sagecoevergreen.app.ui.components.*
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PropertiesScreen(
    initialCategory: String? = null,
    onPropertyClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf(initialCategory ?: "All") }
    var search by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("newest") }
    var offset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var total by remember { mutableStateOf(0) }

    fun load(reset: Boolean = true) {
        scope.launch {
            if (reset) { loading = true; offset = 0 } else loadingMore = true
            error = null
            try {
                val (list, totalCount) = ApiClient.getProperties(
                    category = category,
                    search = search.ifBlank { null },
                    sort = sortBy,
                    limit = 20,
                    offset = if (reset) 0 else offset
                )
                if (reset) properties = list else properties = properties + list
                total = totalCount
                offset = if (reset) 20 else offset + 20
                hasMore = properties.size < totalCount
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            }
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(category, sortBy) { load(true) }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Top bar
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Properties", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(12.dp))
                // Search bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = White
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Search location, title...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = ""; search = ""; load(true) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    TextButton(onClick = {
                        search = searchInput.trim()
                        load(true)
                    }) {
                        Text("Search", color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Residential", "Commercial", "Land", "Plot")) { cat ->
                CategoryChip(cat, category == cat) { category = cat }
            }
        }

        // Sort bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$total properties", fontSize = 13.sp, color = Gray600)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort:", fontSize = 12.sp, color = Gray400)
                Spacer(modifier = Modifier.width(4.dp))
                val sortOptions = listOf("newest" to "Newest", "price_low" to "Price ↑", "price_high" to "Price ↓")
                var sortMenuOpen by remember { mutableStateOf(false) }
                TextButton(onClick = { sortMenuOpen = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(sortOptions.find { it.first == sortBy }?.second ?: "Newest", fontSize = 13.sp, color = SagecoGreen)
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    sortOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { sortBy = value; sortMenuOpen = false }
                        )
                    }
                }
            }
        }

        // Content
        if (loading) {
            LoadingIndicator()
        } else if (error != null) {
            ErrorView(error!!) { load(true) }
        } else if (properties.isEmpty()) {
            EmptyState("No properties found. Try a different search.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(properties) { prop ->
                    PropertyCard(prop) { onPropertyClick(prop.id) }
                }
                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { load(false) }) {
                                if (loadingMore) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SagecoGreen)
                                else Text("Load More", color = SagecoGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
