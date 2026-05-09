package com.lifeos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeos.ui.screens.HealthScreen
import com.lifeos.ui.screens.HistoryScreen
import com.lifeos.ui.screens.HomeScreen
import com.lifeos.ui.screens.permission.OnboardingScreen
import com.lifeos.ui.screens.permission.PermissionSettingsScreen

private const val ROUTE_ONBOARDING = "onboarding"

@Composable
fun AppNavigation(startOnboarding: Boolean = false) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) ROUTE_ONBOARDING else BottomNavItem.Home.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute != ROUTE_ONBOARDING

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_ONBOARDING) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Health.route) { HealthScreen() }
            composable(BottomNavItem.History.route) { HistoryScreen() }
            composable(BottomNavItem.Settings.route) { PermissionSettingsScreen() }
        }
    }
}
