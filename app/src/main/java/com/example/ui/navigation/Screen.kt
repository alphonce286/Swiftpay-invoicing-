package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Invoices : Screen(
        route = "invoices",
        title = "Invoices",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )

    object Clients : Screen(
        route = "clients",
        title = "Clients",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )

    object Reminders : Screen(
        route = "reminders",
        title = "Reminders",
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive
    )

    object CreateInvoice : Screen(
        route = "create_invoice",
        title = "New Invoice"
    )

    object InvoiceDetail : Screen(
        route = "invoice_detail/{invoiceId}",
        title = "Invoice Details"
    ) {
        fun createRoute(invoiceId: Long): String = "invoice_detail/$invoiceId"
    }

    object ClientDetail : Screen(
        route = "client_detail/{clientId}",
        title = "Client Details"
    ) {
        fun createRoute(clientId: Long): String = "client_detail/$clientId"
    }

    object AddClient : Screen(
        route = "add_client",
        title = "Add Client"
    )

    object EditClient : Screen(
        route = "edit_client/{clientId}",
        title = "Edit Client"
    ) {
        fun createRoute(clientId: Long): String = "edit_client/$clientId"
    }

    object Settings : Screen(
        route = "settings",
        title = "Settings"
    )
}
