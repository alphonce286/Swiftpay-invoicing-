package com.example.ui.invoice

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.ui.client.ClientViewModel
import com.example.ui.invoice.components.InvoiceItemAndDueDateEditor
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCreateEditScreen(
    invoiceViewModel: InvoiceViewModel,
    clientViewModel: ClientViewModel,
    onBack: () -> Unit,
    onInvoiceSaved: (Long) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createState by invoiceViewModel.createState.collectAsStateWithLifecycle()
    val clients by invoiceViewModel.clientsList.collectAsStateWithLifecycle()
    val settings by invoiceViewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val symbol = settings.currencySymbol

    var showClientSheet by remember { mutableStateOf(false) }
    var showQuickAddClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientPhone by remember { mutableStateOf("") }
    var newClientEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        invoiceViewModel.initNewInvoice()
    }

    Scaffold(
        modifier = modifier.testTag("create_invoice_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Create Invoice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = createState.invoiceNumber,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("create_invoice_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Sticky Bottom Total & Save Button
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Amount",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(createState.totalAmount, symbol),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            invoiceViewModel.saveInvoice { newId ->
                                Toast.makeText(context, "Invoice created successfully!", Toast.LENGTH_SHORT).show()
                                onInvoiceSaved(newId)
                            }
                        },
                        enabled = !createState.isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_invoice_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (createState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save & Review",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
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
            // Free Tier Limit Warning if reached
            if (createState.isLimitReached) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Free quota (3 invoices) reached. Switch to Pro in Settings for unlimited invoices.",
                                fontSize = 13.sp,
                                color = Color(0xFF92400E),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onUpgradeClick) {
                                Text("Pro", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Error banner if any
            if (createState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("form_validation_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = createState.error ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 1. Client Selection Card (Fast selection)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Client / Bill To *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            TextButton(
                                onClick = { showQuickAddClientDialog = true },
                                modifier = Modifier.testTag("quick_add_client_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ New Client", fontSize = 12.sp)
                            }
                        }

                        if (createState.selectedClient == null) {
                            OutlinedButton(
                                onClick = { showClientSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_client_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select an Existing Client")
                            }
                        } else {
                            val client = createState.selectedClient!!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                    .clickable { showClientSheet = true }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subtitle = listOfNotNull(
                                        client.company.takeIf { it.isNotBlank() },
                                        client.phone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { showClientSheet = true }) {
                                    Text("Change", fontSize = 12.sp)
                                }
                            }
                        }

                        if (createState.clientError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ ${createState.clientError}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("client_error_text")
                            )
                        }
                    }
                }
            }

            // 2. Core UI Component: Invoice Items, Amount, and Due Date Selection
            item {
                InvoiceItemAndDueDateEditor(
                    items = createState.items,
                    dueDate = createState.dueDate,
                    taxPercent = createState.taxPercent,
                    discountPercent = createState.discountPercent,
                    currencySymbol = symbol,
                    dueDateError = createState.dueDateError,
                    itemDescriptionErrors = createState.itemDescriptionErrors,
                    itemPriceErrors = createState.itemPriceErrors,
                    amountError = createState.amountError,
                    onAddItem = { desc, qty, price ->
                        invoiceViewModel.addLineItem(description = desc, qty = qty, price = price)
                    },
                    onUpdateItem = { index, desc, qty, price ->
                        invoiceViewModel.updateLineItem(index, desc, qty, price)
                    },
                    onRemoveItem = { index ->
                        invoiceViewModel.removeLineItem(index)
                    },
                    onDueDateChanged = { newDueDate ->
                        invoiceViewModel.setCustomDueDate(newDueDate)
                    },
                    onTaxChanged = { tax ->
                        invoiceViewModel.updateTaxPercent(tax)
                    },
                    onDiscountChanged = { disc ->
                        invoiceViewModel.updateDiscountPercent(disc)
                    }
                )
            }

            // 3. Notes & Terms Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment Terms & Notes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = createState.notes,
                            onValueChange = { invoiceViewModel.updateNotes(it) },
                            label = { Text("Terms & Notes to Client") },
                            placeholder = { Text("e.g. Payment due within 14 days. Thank you for your business!") },
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_notes_input")
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Modal Sheet: Select Existing Client
    if (showClientSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClientSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Client",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(onClick = {
                        showClientSheet = false
                        showQuickAddClientDialog = true
                    }) {
                        Text("+ New Client")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (clients.isEmpty()) {
                    Text(
                        text = "No clients saved yet. Tap '+ New Client' to add one.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clients) { client ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        invoiceViewModel.setCreateClient(client)
                                        showClientSheet = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    val info = listOfNotNull(
                                        client.company.takeIf { it.isNotBlank() },
                                        client.phone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (info.isNotBlank()) {
                                        Text(
                                            text = info,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (createState.selectedClient?.id == client.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Quick Add Client Dialog
    if (showQuickAddClientDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddClientDialog = false },
            title = { Text("Quick Add Client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = { newClientName = it },
                        label = { Text("Client Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_client_name_input")
                    )

                    OutlinedTextField(
                        value = newClientPhone,
                        onValueChange = { newClientPhone = it },
                        label = { Text("Phone / WhatsApp *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_client_phone_input")
                    )

                    OutlinedTextField(
                        value = newClientEmail,
                        onValueChange = { newClientEmail = it },
                        label = { Text("Email (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClientName.isNotBlank() && newClientPhone.isNotBlank()) {
                            clientViewModel.saveClient(
                                name = newClientName,
                                phone = newClientPhone,
                                email = newClientEmail,
                                company = "",
                                address = "",
                                notes = ""
                            ) { newClientId ->
                                invoiceViewModel.setCreateClient(
                                    ClientEntity(
                                        id = newClientId,
                                        name = newClientName,
                                        phone = newClientPhone,
                                        email = newClientEmail
                                    )
                                )
                                showQuickAddClientDialog = false
                                newClientName = ""
                                newClientPhone = ""
                                newClientEmail = ""
                            }
                        } else {
                            Toast.makeText(context, "Please enter client name and phone number", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_quick_client_btn")
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
