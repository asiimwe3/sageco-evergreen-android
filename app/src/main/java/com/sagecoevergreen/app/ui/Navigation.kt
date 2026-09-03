package com.sagecoevergreen.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sagecoevergreen.app.ui.screens.*
import com.sagecoevergreen.app.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Properties : Screen("properties", "Properties", Icons.Default.Domain)
    data object Brokers : Screen("brokers", "Brokers", Icons.Default.Handshake)
    data object Agents : Screen("agents", "Agents", Icons.Default.Group)
    data object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    data object Account : Screen("account", "Account", Icons.Default.Person)
}

private val bottomNavItems = listOf(Screen.Home, Screen.Properties, Screen.Agents, Screen.Chat, Screen.Account)

@Composable
fun SagecoApp(
    navController: NavHostController,
    savedAgentId: String?,
    onSaveAgentId: (String) -> Unit,
    pendingRoute: String? = null
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(currentRoute) { route ->
                    if (route == currentRoute) return@BottomBar
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onPropertyClick = { id -> navController.navigate("property/$id") },
                    onSeeAllProperties = { navController.navigate("properties") },
                    onNavigate = { route ->
                        if (route.startsWith("properties?category=")) {
                            val cat = route.substringAfter("category=")
                            navController.navigate("properties?category=$cat")
                        } else {
                            navController.navigate(route)
                        }
                    }
                )
            }
            composable(
                "properties?category={category}",
                arguments = listOf(androidx.navigation.navArgument("category") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                val cat = entry.arguments?.getString("category")
                PropertiesScreen(
                    initialCategory = cat,
                    onPropertyClick = { id -> navController.navigate("property/$id") }
                )
            }
            composable("property/{id}") { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                PropertyDetailScreen(
                    propertyId = id,
                    onBack = { navController.popBackStack() },
                    onPropertyClick = { navId -> navController.navigate("property/$navId") }
                )
            }
            // FIX #1: Added missing "brokers" route — was causing crash when tapping "Become a Broker"
            composable("brokers") {
                BrokersScreen()
            }
            composable("agents") {
                AgentsScreen(savedAgentId = savedAgentId, onSaveAgentId = onSaveAgentId)
            }
            composable("chat") {
                ChatScreen()
            }
            composable("account") {
                AccountScreen(savedAgentId = savedAgentId, onNavigate = { route ->
                    navController.navigate(route)
                })
            }
        }
    }

    // Handle pending route from notification/deep link
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute)
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onItemClick: (String) -> Unit) {
    NavigationBar(
        containerColor = SagecoGreen,
        contentColor = White,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(screen.route) },
                icon = { Icon(screen.icon, null, modifier = Modifier.size(22.dp)) },
                label = { Text(screen.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = SagecoGreen
                )
            )
        }
    }
}
