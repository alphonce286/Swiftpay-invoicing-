package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InvoiceCard
import com.example.ui.components.ProTierBanner
import com.example.ui.dashboard.components.InvoiceSummaryCards
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedDark
import com.example.ui.theme.OverdueRedLight
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenLight
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberLight
import com.example.ui.theme.PrimaryBlue
import com.example.util.CurrencyFormatter
import com.example.util.ReminderTemplateHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateInvoiceClick: () -> Unit,
    onViewInvoicesClick: () -> Unit,
    onViewRemindersClick: () -> Unit,
    onViewClientsClick: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val symbol = uiState.settings.currencySymbol

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.settings.businessName.ifEmpty { "InvoiceFlow" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Freelance & Small Business Invoicing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("dashboard_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateInvoiceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("dashboard_create_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Invoice")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Invoice", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overdue Urgent Alert Banner
            if (uiState.overdueCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onViewRemindersClick() }
                            .testTag("urgent_overdue_banner"),
                        colors = CardDefaults.cardColors(containerColor = OverdueRedLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OverdueRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Overdue alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${uiState.overdueCount} Invoice${if (uiState.overdueCount > 1) "s" else ""} Overdue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OverdueRedDark
                                )
                                Text(
                                    text = "Total overdue: ${CurrencyFormatter.format(uiState.overdueTotal, symbol)} • Tap to send 1-tap reminder",
                                    fontSize = 12.sp,
                                    color = OverdueRedDark.copy(alpha = 0.85f)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View reminders",
                                tint = OverdueRedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. MAIN MATERIAL 3 SUMMARY STATISTIC CARDS (PAID, PENDING, OVERDUE)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cashflow Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Live status",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                InvoiceSummaryCards(
                    paidTotal = uiState.paidTotal,
                    paidCount = uiState.paidCount,
                    pendingTotal = uiState.pendingTotal,
                    pendingCount = uiState.pendingCount,
                    overdueTotal = uiState.overdueTotal,
                    overdueCount = uiState.overdueCount,
                    currencySymbol = symbol,
                    onOverdueActionClick = onViewRemindersClick,
                    onCardClick = { onViewInvoicesClick() }
                )
            }

            // 3. Pro Plan Status / Quota Banner
            item {
                ProTierBanner(
                    invoicesCount = uiState.totalInvoicesCount,
                    isPro = uiState.settings.isProTier,
                    limit = uiState.settings.freeInvoicesLimit,
                    onUpgradeClick = onSettingsClick
                )
            }

            // 4. Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewRemindersClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_reminders_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.overdueCount > 0) "Reminders (${uiState.overdueCount})" else "Reminders",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onViewClientsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_clients_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clients", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5. Recent Invoices Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Invoices",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onViewInvoicesClick,
                        modifier = Modifier.testTag("see_all_invoices_btn")
                    ) {
                        Text("See All (${uiState.totalInvoicesCount})", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 6. Recent Invoices List
            if (uiState.recentInvoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No invoices created yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create an invoice in under a minute and share it via WhatsApp, SMS, or Email.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateInvoiceClick,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create First Invoice")
                            }
                        }
                    }
                }
            } else {
                items(uiState.recentInvoices, key = { it.invoice.id }) { invoiceWithDetails ->
                    InvoiceCard(
                        invoiceWithDetails = invoiceWithDetails,
                        currencySymbol = symbol,
                        onClick = { onInvoiceClick(invoiceWithDetails.invoice.id) },
                        onQuickShare = {
                            val text = ReminderTemplateHelper.generateQuickShareText(
                                invoiceWithDetails = invoiceWithDetails,
                                settings = uiState.settings
                            )
                            val phone = invoiceWithDetails.client?.phone ?: ""
                            SharingHelper.shareViaWhatsApp(context, phone, text)
                        }
                    )
                }
            }

            // Bottom space for FAB & Navigation bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
