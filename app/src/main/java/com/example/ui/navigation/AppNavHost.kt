package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.client.AddEditClientScreen
import com.example.ui.client.ClientDetailScreen
import com.example.ui.client.ClientListScreen
import com.example.ui.client.ClientViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.invoice.InvoiceCreateEditScreen
import com.example.ui.invoice.InvoiceDetailScreen
import com.example.ui.invoice.InvoiceListScreen
import com.example.ui.invoice.InvoiceViewModel
import com.example.ui.reminder.RemindersScreen
import com.example.ui.reminder.RemindersViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

@Composable
fun AppNavHost(
    initialInvoiceId: Long? = null,
    navController: NavHostController = rememberNavController(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    invoiceViewModel: InvoiceViewModel = viewModel(),
    clientViewModel: ClientViewModel = viewModel(),
    remindersViewModel: RemindersViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(initialInvoiceId) {
        if (initialInvoiceId != null && initialInvoiceId > 0) {
            navController.navigate(Screen.InvoiceDetail.createRoute(initialInvoiceId))
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Invoices,
        Screen.Clients,
        Screen.Reminders
    )

    val isBottomBarVisible = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onCreateInvoiceClick = { navController.navigate(Screen.CreateInvoice.route) },
                    onViewInvoicesClick = { navController.navigate(Screen.Invoices.route) },
                    onViewRemindersClick = { navController.navigate(Screen.Reminders.route) },
                    onViewClientsClick = { navController.navigate(Screen.Clients.route) },
                    onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Invoices.route) {
                InvoiceListScreen(
                    viewModel = invoiceViewModel,
                    onCreateInvoiceClick = { navController.navigate(Screen.CreateInvoice.route) },
                    onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) }
                )
            }

            composable(Screen.Clients.route) {
                ClientListScreen(
                    viewModel = clientViewModel,
                    onClientClick = { id -> navController.navigate(Screen.ClientDetail.createRoute(id)) },
                    onAddClientClick = { navController.navigate(Screen.AddClient.route) }
                )
            }

            composable(Screen.AddClient.route) {
                AddEditClientScreen(
                    clientId = 0L,
                    viewModel = clientViewModel,
                    onBack = { navController.popBackStack() },
                    onClientSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditClient.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                AddEditClientScreen(
                    clientId = clientId,
                    viewModel = clientViewModel,
                    onBack = { navController.popBackStack() },
                    onClientSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    viewModel = remindersViewModel,
                    onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) }
                )
            }

            composable(Screen.CreateInvoice.route) {
                InvoiceCreateEditScreen(
                    invoiceViewModel = invoiceViewModel,
                    clientViewModel = clientViewModel,
                    onBack = { navController.popBackStack() },
                    onInvoiceSaved = { id ->
                        navController.navigate(Screen.InvoiceDetail.createRoute(id)) {
                            popUpTo(Screen.Invoices.route) {
                                inclusive = false
                            }
                        }
                    },
                    onUpgradeClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.InvoiceDetail.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    viewModel = invoiceViewModel,
                    onBack = { navController.popBackStack() },
                    onClientClick = { clientId -> navController.navigate(Screen.ClientDetail.createRoute(clientId)) }
                )
            }

            composable(
                route = Screen.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                ClientDetailScreen(
                    clientId = clientId,
                    clientViewModel = clientViewModel,
                    onBack = { navController.popBackStack() },
                    onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) },
                    onEditClientClick = { id -> navController.navigate(Screen.EditClient.createRoute(id)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
