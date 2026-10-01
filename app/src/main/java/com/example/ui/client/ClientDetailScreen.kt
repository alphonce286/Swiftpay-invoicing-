package com.example.ui.client

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.ui.components.InvoiceCard
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingAmber
import com.example.util.CurrencyFormatter
import com.example.util.ReminderTemplateHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    clientViewModel: ClientViewModel,
    onBack: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onEditClientClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by clientViewModel.uiState.collectAsStateWithLifecycle()
    val client = uiState.clients.find { it.id == clientId }
    val clientInvoicesFlow = remember(clientId) { clientViewModel.getClientInvoices(clientId) }
    val clientInvoices by clientInvoicesFlow.collectAsStateWithLifecycle()
    val settings by clientViewModel.settings.collectAsStateWithLifecycle()
    val symbol = settings?.currencySymbol ?: "$"
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Client not found")
        }
        return
    }

    val totalPaid = clientInvoices.filter { it.invoice.status == "PAID" }.sumOf { it.invoice.totalAmount }
    val totalPending = clientInvoices.filter { it.invoice.status != "PAID" }.sumOf { it.invoice.totalAmount }

    Scaffold(
        modifier = modifier.testTag("client_detail_screen"),
        topBar = {
            TopAppBar(
                title = { Text(client.name, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEditClientClick(client.id) },
                        modifier = Modifier.testTag("edit_client_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Client")
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_client_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Client Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = client.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(text = client.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                if (client.company.isNotBlank()) {
                                    Text(text = client.company, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Contact Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (client.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { SharingHelper.shareViaWhatsApp(context, client.phone, "Hello ${client.name}!") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call", fontSize = 12.sp)
                                }
                            }

                            if (client.email.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        SharingHelper.shareViaEmail(context, client.email, "Invoice Query", "Hello ${client.name},\n")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email", fontSize = 12.sp)
                                }
                            }
                        }

                        if (client.address.isNotBlank() || client.notes.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            if (client.address.isNotBlank()) {
                                Text("Address: ${client.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (client.notes.isNotBlank()) {
                                Text("Notes: ${client.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Billing Totals with this client
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(totalPaid, symbol), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaidGreen)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Outstanding", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(totalPending, symbol), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PendingAmber)
                        }
                    }
                }
            }

            // Invoices for this client
            item {
                Text(
                    text = "Invoices (${clientInvoices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (clientInvoices.isEmpty()) {
                item {
                    Text(
                        text = "No invoices issued for this client yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(clientInvoices, key = { it.invoice.id }) { invoiceWithDetails ->
                    InvoiceCard(
                        invoiceWithDetails = invoiceWithDetails,
                        currencySymbol = symbol,
                        onClick = { onInvoiceClick(invoiceWithDetails.invoice.id) },
                        onQuickShare = {
                            val safeSettings = settings ?: com.example.data.local.entity.PaymentSettingsEntity()
                            val text = ReminderTemplateHelper.generateQuickShareText(invoiceWithDetails, safeSettings)
                            SharingHelper.shareViaWhatsApp(context, client.phone, text)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Client?") },
            text = { Text("Are you sure you want to delete ${client.name}? Their existing invoices will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        clientViewModel.deleteClient(client)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
