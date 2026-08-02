package com.norvexa.flow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.norvexa.flow.ui.dashboard.DashboardScreen
import com.norvexa.flow.ui.dashboard.DashboardViewModel

private enum class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Overview("overview", "Overview", Icons.Outlined.SpaceDashboard),
    Calendar("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    Clients("clients", "Clients", Icons.Outlined.Groups),
    Reports("reports", "Reports", Icons.Outlined.Assessment),
    More("more", "More", Icons.Outlined.MoreHoriz),
}

@Composable
fun NorvexaFlowApp() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Overview.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Overview.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = state,
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddReceivable = {},
                    onOpenPricing = {},
                )
            }
            composable(Destination.Calendar.route) {
                PlaceholderScreen("Cash-flow calendar")
            }
            composable(Destination.Clients.route) {
                PlaceholderScreen("Clients and expected payments")
            }
            composable(Destination.Reports.route) {
                PlaceholderScreen("Reports")
            }
            composable(Destination.More.route) {
                PlaceholderScreen("Calculators, reserves and settings")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
