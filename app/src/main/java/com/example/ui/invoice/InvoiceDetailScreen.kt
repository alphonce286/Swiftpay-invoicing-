package com.example.ui.invoice

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.StatusBadge
import com.example.ui.invoice.components.InvoiceShareSheet
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenLight
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
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: InvoiceViewModel,
    onBack: () -> Unit,
    onClientClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val invoiceFlow = remember(invoiceId) { viewModel.getInvoiceDetails(invoiceId) }
    val invoiceWithDetails by invoiceFlow.collectAsStateWithLifecycle(initialValue = null)
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val symbol = settings.currencySymbol

    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var selectedPaidMethod by remember { mutableStateOf("M-Pesa / Mobile Money") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    var showReminderSheet by remember { mutableStateOf(false) }
    var reminderTone by remember { mutableStateOf(ReminderTone.PROFESSIONAL) }
    var reminderTiming by remember { mutableStateOf(ReminderTiming.BEFORE_DUE) }

    if (invoiceWithDetails == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading invoice details...")
        }
        return
    }

    val detail = invoiceWithDetails!!
    val invoice = detail.invoice
    val client = detail.client

    Scaffold(
        modifier = modifier.testTag("invoice_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = invoice.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShareSheet = true },
                        modifier = Modifier.testTag("top_bar_share_invoice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Invoice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_invoice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Invoice",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (invoice.status != "PAID") {
                        OutlinedButton(
                            onClick = {
                                reminderTiming = when {
                                    invoice.status == "OVERDUE" -> ReminderTiming.LATE_3_DAYS
                                    else -> ReminderTiming.BEFORE_DUE
                                }
                                showReminderSheet = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_reminder_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Reminder", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("mark_paid_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaidGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark as Paid", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val pdfUri = viewModel.generatePdf(detail)
                                if (pdfUri != null) {
                                    SharingHelper.sharePdfFile(context, pdfUri, invoice.invoiceNumber)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Paid Receipt / PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paid Banner if paid
            if (invoice.status == "PAID") {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PaidGreenLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PaidGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Payment Completed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Paid on ${CurrencyFormatter.formatDate(invoice.paidDate ?: invoice.createdAt)} via ${invoice.paymentMethod.ifBlank { "Recorded Payment" }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF065F46).copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Primary Share Actions (Deep Link, WhatsApp, SMS, Email, PDF)
            item {
                val shareBundle = remember(detail, settings) {
                    SharingHelper.buildShareBundle(detail, settings)
                }

                Card(
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
                                text = "Share Invoice with Client",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            TextButton(onClick = { showShareSheet = true }) {
                                Text("All Options", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Deep Link display with copy button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Generated Deep Link:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = shareBundle.deepLink,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { SharingHelper.copyDeepLink(context, shareBundle.deepLink) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("copy_deep_link_card_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy link",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // WhatsApp with Deep Link
                            OutlinedButton(
                                onClick = {
                                    SharingHelper.shareViaWhatsApp(context, client?.phone ?: "", shareBundle.whatsAppMessage)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_whatsapp_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF25D366)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp)
                            }

                            // SMS with Deep Link
                            OutlinedButton(
                                onClick = {
                                    SharingHelper.shareViaSms(context, client?.phone ?: "", shareBundle.smsBody)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_sms_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SMS", fontSize = 12.sp)
                            }

                            // Email with Deep Link & PDF
                            OutlinedButton(
                                onClick = {
                                    val pdfUri = viewModel.generatePdf(detail)
                                    SharingHelper.shareViaEmail(
                                        context = context,
                                        recipientEmail = client?.email ?: "",
                                        subject = shareBundle.subject,
                                        bodyText = shareBundle.emailBody,
                                        pdfUri = pdfUri
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_email_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFEA4335)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full Share Options Button
                        Button(
                            onClick = { showShareSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_pdf_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Share Options & PDF Export",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Client & Due Date Summary
            item {
                Card(
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
                            Column {
                                Text(
                                    text = "Billed To",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = client?.name ?: "No client assigned",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (!client?.company.isNullOrBlank()) {
                                    Text(
                                        text = client?.company ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (client != null) {
                                TextButton(onClick = { onClientClick(client.id) }) {
                                    Text("Client Profile", fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Issue Date",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatDate(invoice.issueDate),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Due Date",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val (dueText, isUrgent) = CurrencyFormatter.getDueDateLabel(invoice.dueDate, invoice.status)
                                Text(
                                    text = "${CurrencyFormatter.formatDate(invoice.dueDate)} ($dueText)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Online Payment Link & Mobile Money Details
            item {
                Card(
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Payment Methods Attached",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Online Payment Link
                        if (settings.onlinePaymentUrl.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Card / Online Payment Link", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                                    Text(
                                        settings.onlinePaymentUrl,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = {
                                    SharingHelper.copyToClipboard(context, settings.onlinePaymentUrl, "Payment Link")
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy link", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Mobile Money
                        if (settings.mobileMoneyNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(settings.mobileMoneyProvider, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PaidGreen)
                                    Text(settings.mobileMoneyNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = {
                                    SharingHelper.copyToClipboard(context, settings.mobileMoneyNumber, "Mobile Money Details")
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Bank Transfer
                        if (settings.bankAccountNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bank Transfer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${settings.bankName}: ${settings.bankAccountNumber} (${settings.bankAccountName})", fontSize = 12.sp)
                                }
                                IconButton(onClick = {
                                    SharingHelper.copyToClipboard(context, "${settings.bankName}: ${settings.bankAccountNumber} (${settings.bankAccountName})", "Bank Details")
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Line Items Table & Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Line Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        detail.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.description,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${item.quantity.toInt()} x ${CurrencyFormatter.format(item.unitPrice, symbol)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = CurrencyFormatter.format(item.amount, symbol),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Totals summary
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(invoice.subtotal, symbol), fontSize = 12.sp)
                        }

                        if (invoice.discountPercent > 0) {
                            val disc = invoice.subtotal * (invoice.discountPercent / 100.0)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount (${invoice.discountPercent}%)", fontSize = 12.sp, color = PaidGreen)
                                Text("-${CurrencyFormatter.format(disc, symbol)}", fontSize = 12.sp, color = PaidGreen)
                            }
                        }

                        if (invoice.taxPercent > 0) {
                            val tax = invoice.subtotal * (invoice.taxPercent / 100.0)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax (${invoice.taxPercent}%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+${CurrencyFormatter.format(tax, symbol)}", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Due",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyFormatter.format(invoice.totalAmount, symbol),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (invoice.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Notes / Terms: ${invoice.notes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Modal Sheet: Send Reminder Composer
    if (showReminderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Send Payment Reminder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Select Timing Stage
                Text("Reminder Stage:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderTiming.values().forEach { timing ->
                        FilterChip(
                            selected = reminderTiming == timing,
                            onClick = { reminderTiming = timing },
                            label = {
                                Text(
                                    when (timing) {
                                        ReminderTiming.BEFORE_DUE -> "Before Due"
                                        ReminderTiming.ON_DUE -> "Due Today"
                                        ReminderTiming.LATE_3_DAYS -> "3 Days Late"
                                        ReminderTiming.LATE_7_DAYS -> "7 Days Late"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // Select Tone
                Text("Message Tone:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderTone.values().forEach { tone ->
                        FilterChip(
                            selected = reminderTone == tone,
                            onClick = { reminderTone = tone },
                            label = { Text(tone.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                        )
                    }
                }

                val previewMessage = ReminderTemplateHelper.generateMessage(
                    invoiceWithDetails = detail,
                    settings = settings,
                    timing = reminderTiming,
                    tone = reminderTone
                )

                // Message Preview Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewMessage,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Send Action Buttons (WhatsApp / SMS / Email / Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.logReminderSent(detail, "WHATSAPP", reminderTiming.name, previewMessage)
                            SharingHelper.shareViaWhatsApp(context, client?.phone ?: "", previewMessage)
                            showReminderSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.logReminderSent(detail, "SMS", reminderTiming.name, previewMessage)
                            SharingHelper.shareViaSms(context, client?.phone ?: "", previewMessage)
                            showReminderSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SMS", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.logReminderSent(detail, "EMAIL", reminderTiming.name, previewMessage)
                            val subject = "Reminder: Invoice ${invoice.invoiceNumber} payment"
                            SharingHelper.shareViaEmail(context, client?.email ?: "", subject, previewMessage)
                            showReminderSheet = false
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

    // Mark as Paid Dialog
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text("Confirm Payment Received") },
            text = {
                Column {
                    Text(
                        text = "Total: ${CurrencyFormatter.format(invoice.totalAmount, symbol)} from ${client?.name ?: "Client"}",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Payment Method:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    listOf(
                        "M-Pesa / Mobile Money",
                        "Online Card Payment",
                        "Bank Transfer",
                        "Cash",
                        "Other"
                    ).forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaidMethod = method }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPaidMethod == method,
                                onClick = { selectedPaidMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(method, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markPaid(invoice.id, selectedPaidMethod)
                        showMarkPaidDialog = false
                        Toast.makeText(context, "Invoice marked as paid!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaidGreen)
                ) {
                    Text("Confirm Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Invoice Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Invoice?") },
            text = { Text("Are you sure you want to delete invoice ${invoice.invoiceNumber}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(invoice)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Sheet: Share Options & Deep Link
    if (showShareSheet) {
        InvoiceShareSheet(
            invoiceWithDetails = detail,
            settings = settings,
            pdfUri = viewModel.generatePdf(detail),
            onDismiss = { showShareSheet = false }
        )
    }
}
