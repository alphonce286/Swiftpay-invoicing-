package com.example.ui.reminder

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.InvoiceWithDetails
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedLight
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberLight
import com.example.ui.theme.PrimaryBlue
import com.example.util.CurrencyFormatter
import com.example.util.ReminderTiming
import com.example.util.ReminderTone
import com.example.util.ReminderTemplateHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onInvoiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val symbol = uiState.settings.currencySymbol

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activeReminderItem by remember { mutableStateOf<InvoiceWithDetails?>(null) }
    var selectedTone by remember { mutableStateOf(ReminderTone.PROFESSIONAL) }
    var selectedTiming by remember { mutableStateOf(ReminderTiming.BEFORE_DUE) }

    Scaffold(
        modifier = modifier.testTag("reminders_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Payment Reminders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "${uiState.pendingReminders.size} invoices awaiting payment",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Needs Follow-up (${uiState.pendingReminders.size})", fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_follow_ups")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Reminder History (${uiState.reminderLogs.size})", fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            if (selectedTabIndex == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Schedule Guide Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Automatic Reminder Cadence", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "1. Before Due Date (2 days before) • 2. On Due Date • 3. 3 Days Late • 4. 7 Days Late",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.pendingReminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PaidGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("All caught up!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "No unpaid invoices currently need reminders.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.pendingReminders) { item ->
                            val invoiceWithDetails = item.invoiceWithDetails
                            val inv = invoiceWithDetails.invoice
                            val client = invoiceWithDetails.client

                            val (stageBadgeBg, stageBadgeText, stageTitle) = when (item.suggestedTiming) {
                                ReminderTiming.LATE_7_DAYS -> Triple(OverdueRedLight, OverdueRed, "7 Days Overdue (Urgent)")
                                ReminderTiming.LATE_3_DAYS -> Triple(OverdueRedLight, OverdueRed, "3 Days Overdue")
                                ReminderTiming.ON_DUE -> Triple(PendingAmberLight, PendingAmber, "Due Today")
                                ReminderTiming.BEFORE_DUE -> Triple(Color(0xFFDBEAFE), PrimaryBlue, "Upcoming Due Date")
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_card_${inv.invoiceNumber}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inv.invoiceNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = stageBadgeBg
                                        ) {
                                            Text(
                                                text = stageTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = stageBadgeText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = client?.name ?: "No client",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Due: ${CurrencyFormatter.formatDate(inv.dueDate)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = CurrencyFormatter.format(inv.totalAmount, symbol),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 1-Tap Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Quick WhatsApp
                                        Button(
                                            onClick = {
                                                val msg = ReminderTemplateHelper.generateMessage(
                                                    invoiceWithDetails = invoiceWithDetails,
                                                    settings = uiState.settings,
                                                    timing = item.suggestedTiming,
                                                    tone = ReminderTone.PROFESSIONAL
                                                )
                                                viewModel.logSentReminder(invoiceWithDetails, "WHATSAPP", item.suggestedTiming, msg)
                                                SharingHelper.shareViaWhatsApp(context, client?.phone ?: "", msg)
                                            },
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 12.sp)
                                        }

                                        // Quick SMS
                                        OutlinedButton(
                                            onClick = {
                                                val msg = ReminderTemplateHelper.generateMessage(
                                                    invoiceWithDetails = invoiceWithDetails,
                                                    settings = uiState.settings,
                                                    timing = item.suggestedTiming,
                                                    tone = ReminderTone.PROFESSIONAL
                                                )
                                                viewModel.logSentReminder(invoiceWithDetails, "SMS", item.suggestedTiming, msg)
                                                SharingHelper.shareViaSms(context, client?.phone ?: "", msg)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SMS", fontSize = 12.sp)
                                        }

                                        // Customize
                                        IconButton(
                                            onClick = {
                                                activeReminderItem = invoiceWithDetails
                                                selectedTiming = item.suggestedTiming
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Customize")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Reminder Logs History Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.reminderLogs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reminders sent yet. Reminders you send via WhatsApp, SMS, or Email will appear here.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.reminderLogs, key = { it.id }) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PaidGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${log.channel} • ${log.invoiceNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Text(
                                            text = CurrencyFormatter.formatShortDate(log.sentAt),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To ${log.clientName} (${CurrencyFormatter.format(log.amount, symbol)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.messageText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Customize & Send Reminder
    if (activeReminderItem != null) {
        val item = activeReminderItem!!
        val inv = item.invoice
        val client = item.client

        ModalBottomSheet(
            onDismissRequest = { activeReminderItem = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Follow up with ${client?.name ?: "Client"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Select Tone
                Text("Select Tone:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderTone.values().forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { selectedTone = tone },
                            label = { Text(tone.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                val msg = ReminderTemplateHelper.generateMessage(
                    invoiceWithDetails = item,
                    settings = uiState.settings,
                    timing = selectedTiming,
                    tone = selectedTone
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.logSentReminder(item, "WHATSAPP", selectedTiming, msg)
                            SharingHelper.shareViaWhatsApp(context, client?.phone ?: "", msg)
                            activeReminderItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.logSentReminder(item, "SMS", selectedTiming, msg)
                            SharingHelper.shareViaSms(context, client?.phone ?: "", msg)
                            activeReminderItem = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SMS", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.logSentReminder(item, "EMAIL", selectedTiming, msg)
                            SharingHelper.shareViaEmail(context, client?.email ?: "", "Invoice Reminder ${inv.invoiceNumber}", msg)
                            activeReminderItem = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Email", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
