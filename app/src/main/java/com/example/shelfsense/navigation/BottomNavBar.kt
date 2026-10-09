package com.example.shelfsense.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.example.shelfsense.ui.theme.ShelfTheme

// the four top level destinations, set up like the unit's NavBarItem list
data class NavBarItem(val label: String, val icon: ImageVector, val route: String, val target: String)

val navBarItems = listOf(
    NavBarItem("Home", Icons.Filled.Home, Routes.HOME, Routes.HOME),
    NavBarItem("Pantry", Icons.AutoMirrored.Filled.List, Routes.PANTRY, Routes.pantry()),
    NavBarItem("Insights", Icons.Filled.BarChart, Routes.INSIGHTS, Routes.INSIGHTS),
    NavBarItem("Profile", Icons.Filled.Person, Routes.PROFILE, Routes.PROFILE)
)

val topLevelRoutes = navBarItems.map { it.route }.toSet()

@Composable
fun ShelfBottomBar(navController: NavHostController, destination: NavDestination?) {
    val c = ShelfTheme.colors
    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
        navBarItems.forEach { item ->
            val selected = destination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.target) {
                            // standard tab behaviour: one copy of each tab, and its state kept when switching
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = {
                    Text(
                        item.label,
                        style = if (selected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall
                    )
                },
                // as in the prototype, the selected tab is carried by colour and weight with no pill behind it
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.primary,
                    selectedTextColor = c.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = c.muted,
                    unselectedTextColor = c.muted
                )
            )
        }
    }
}
