package com.example.ui.invoice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.invoice.LineItemDraft
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PrimaryBlue
import com.example.util.CurrencyFormatter
import java.util.Calendar

/**
 * Reusable Material 3 UI component for the 'Create Invoice' screen,
 * encapsulating line items entry, due date selection with M3 DatePicker,
 * and live amount breakdown (subtotal, tax %, discount %, and grand total).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceItemAndDueDateEditor(
    items: List<LineItemDraft>,
    dueDate: Long,
    taxPercent: Double,
    discountPercent: Double,
    currencySymbol: String,
    onAddItem: (description: String, qty: Double, price: Double) -> Unit,
    onUpdateItem: (index: Int, description: String, qty: Double, price: Double) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onDueDateChanged: (Long) -> Unit,
    onTaxChanged: (Double) -> Unit,
    onDiscountChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
    dueDateError: String? = null,
    itemDescriptionErrors: Set<Int> = emptySet(),
    itemPriceErrors: Set<Int> = emptySet(),
    amountError: String? = null
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)

    val subtotal by remember(items) {
        derivedStateOf { items.sumOf { it.amount } }
    }
    val discountAmount by remember(subtotal, discountPercent) {
        derivedStateOf { subtotal * (discountPercent / 100.0) }
    }
    val taxAmount by remember(subtotal, discountAmount, taxPercent) {
        derivedStateOf { (subtotal - discountAmount) * (taxPercent / 100.0) }
    }
    val grandTotal by remember(subtotal, discountAmount, taxAmount) {
        derivedStateOf { maxOf(0.0, subtotal - discountAmount + taxAmount) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_item_and_due_date_editor"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. MATERIAL 3 DUE DATE SELECTION COMPONENT
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.testTag("due_date_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Payment Due Date",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val (dueLabel, isUrgent) = CurrencyFormatter.getDueDateLabel(dueDate, "PENDING")
                            Text(
                                text = dueLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Clickable chip/button to open Material 3 DatePickerDialog
                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_date_picker_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Pick Date",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatDate(dueDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shortcut presets (+0 days, +7 days, +14 days, +30 days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = System.currentTimeMillis()
                    val dayMs = 86_400_000L

                    listOf(
                        "Today" to 0,
                        "+7 Days" to 7,
                        "+14 Days" to 14,
                        "+30 Days" to 30
                    ).forEach { (label, days) ->
                        val targetTime = now + (days * dayMs)
                        val isSelected = (dueDate - now) / dayMs in (days - 1)..days

                        FilterChip(
                            selected = isSelected,
                            onClick = { onDueDateChanged(targetTime) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("due_date_chip_$days"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (dueDateError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ $dueDateError",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("due_date_error_text")
                    )
                }
            }
        }

        // ==========================================
        // 2. MATERIAL 3 INVOICE ITEMS COMPONENT
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.testTag("invoice_items_section")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Line Items (${items.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Subtotal: ${CurrencyFormatter.format(subtotal, currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast preset service chips for freelancers/shops
                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val presets = listOf(
                        Triple("UI Design", 1.0, 350.0),
                        Triple("Web Development", 1.0, 600.0),
                        Triple("Consulting (hr)", 4.0, 75.0),
                        Triple("Maintenance", 1.0, 150.0),
                        Triple("Copywriting", 1.0, 200.0)
                    )
                    items(presets) { (title, qty, price) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onAddItem(title, qty, price) }
                                .testTag("preset_$title")
                        ) {
                            Text(
                                text = "+ $title",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Line items list with Material 3 Input components
                items.forEachIndexed { index, item ->
                    InvoiceItemCard(
                        index = index,
                        item = item,
                        currencySymbol = currencySymbol,
                        canDelete = items.size > 1,
                        isDescriptionError = index in itemDescriptionErrors,
                        isPriceError = index in itemPriceErrors,
                        onUpdate = { desc, q, rate -> onUpdateItem(index, desc, q, rate) },
                        onDelete = { onRemoveItem(index) }
                    )
                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "+ Add Item" Material 3 OutlinedButton
                OutlinedButton(
                    onClick = { onAddItem("Professional Services", 1.0, 0.0) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Line Item", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ==========================================
        // 3. MATERIAL 3 TAX, DISCOUNT & TOTAL AMOUNT
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.testTag("amount_summary_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Amount & Adjustments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Material 3 Tax & Discount input fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = if (taxPercent == 0.0) "" else taxPercent.toString().removeSuffix(".0"),
                        onValueChange = { onTaxChanged(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Tax / VAT") },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_tax_percent")
                    )

                    OutlinedTextField(
                        value = if (discountPercent == 0.0) "" else discountPercent.toString().removeSuffix(".0"),
                        onValueChange = { onDiscountChanged(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Discount") },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_discount_percent")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.format(subtotal, currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (discountPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discount (${discountPercent}%)", fontSize = 13.sp, color = PaidGreen)
                        Text("-${CurrencyFormatter.format(discountAmount, currencySymbol)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PaidGreen)
                    }
                }

                if (taxPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax / VAT (${taxPercent}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${CurrencyFormatter.format(taxAmount, currencySymbol)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grand Total Highlight Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Amount client will pay",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(grandTotal, currencySymbol),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (amountError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ $amountError",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("amount_error_text")
                    )
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedTimestamp ->
                            onDueDateChanged(selectedTimestamp)
                        }
                        showDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_btn")
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Individual Line Item Card built with Material 3 input components:
 * OutlinedTextField with clean labels, prefix, suffix, and delete action.
 */
@Composable
fun InvoiceItemCard(
    index: Int,
    item: LineItemDraft,
    currencySymbol: String,
    canDelete: Boolean,
    onUpdate: (description: String, quantity: Double, price: Double) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDescriptionError: Boolean = false,
    isPriceError: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("line_item_card_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Description input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.description,
                    onValueChange = { onUpdate(it, item.quantity, item.unitPrice) },
                    label = { Text("Item / Service Description *") },
                    placeholder = { Text("e.g. Website Design & Development") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = if (isDescriptionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    },
                    isError = isDescriptionError,
                    supportingText = if (isDescriptionError) {
                        { Text("Item description is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_description_$index")
                )

                if (canDelete) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_item_btn_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity, Unit Rate, and Live Item Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity field
                OutlinedTextField(
                    value = if (item.quantity == 0.0) "" else item.quantity.toString().removeSuffix(".0"),
                    onValueChange = {
                        val q = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.description, q, item.unitPrice)
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isPriceError && item.quantity <= 0.0,
                    supportingText = if (isPriceError && item.quantity <= 0.0) {
                        { Text("> 0", color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("item_qty_$index")
                )

                // Rate / Price field with currency prefix
                OutlinedTextField(
                    value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString().removeSuffix(".0"),
                    onValueChange = {
                        val p = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.description, item.quantity, p)
                    },
                    label = { Text("Rate") },
                    prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isPriceError && item.unitPrice <= 0.0,
                    supportingText = if (isPriceError && item.unitPrice <= 0.0) {
                        { Text("Required (> 0)", color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("item_price_$index")
                )

                // Calculated Item Amount Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Amount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(item.amount, currencySymbol),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
