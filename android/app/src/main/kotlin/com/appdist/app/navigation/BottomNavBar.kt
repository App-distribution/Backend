package com.appdist.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavDestination("home", "Home", Icons.Default.Home)
    data object Browse : BottomNavDestination("browse", "Browse", Icons.Default.Search)
    data object Mine : BottomNavDestination("mine", "Mine", Icons.Default.DownloadDone)
    data object Profile : BottomNavDestination("profile", "Profile", Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavDestination.Home,
    BottomNavDestination.Browse,
    BottomNavDestination.Mine,
    BottomNavDestination.Profile
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
