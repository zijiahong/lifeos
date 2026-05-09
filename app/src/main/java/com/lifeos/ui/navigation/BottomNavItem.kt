package com.lifeos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "首页", Icons.Default.Home)
    data object Health : BottomNavItem("health", "健康", Icons.Default.Favorite)
    data object History : BottomNavItem("history", "历史", Icons.Default.History)
    data object Settings : BottomNavItem("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Health,
    BottomNavItem.History,
    BottomNavItem.Settings
)
