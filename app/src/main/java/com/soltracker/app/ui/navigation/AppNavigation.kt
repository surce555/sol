package com.soltracker.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soltracker.app.ui.screen.AlertScreen
import com.soltracker.app.ui.screen.ChartScreen
import com.soltracker.app.ui.screen.HomeScreen
import com.soltracker.app.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "行情")
    object Chart : Screen("chart", "K线")
    object Alerts : Screen("alerts", "提醒")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    val screens = listOf(Screen.Home, Screen.Chart, Screen.Alerts)
    val icons = mapOf(
        Screen.Home.route to Icons.Filled.Home,
        Screen.Chart.route to Icons.Filled.BarChart,
        Screen.Alerts.route to Icons.Filled.Notifications
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(icons[screen.route]!!, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel)
            }
            composable(Screen.Chart.route) {
                ChartScreen(viewModel = viewModel)
            }
            composable(Screen.Alerts.route) {
                AlertScreen(viewModel = viewModel)
            }
        }
    }
}
