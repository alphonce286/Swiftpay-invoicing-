package com.example.ui.invoice

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.data.local.entity.ReminderLogEntity
import com.example.data.repository.ClientRepository
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.PaymentSettingsRepository
import com.example.model.InvoiceWithDetails
import com.example.util.PdfInvoiceGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InvoiceListUiState(
    val invoices: List<InvoiceWithDetails> = emptyList(),
    val filteredInvoices: List<InvoiceWithDetails> = emptyList(),
    val selectedFilter: String = "ALL", // ALL, PENDING, OVERDUE, PAID
    val searchQuery: String = "",
    val settings: PaymentSettingsEntity = PaymentSettingsEntity()
)

data class LineItemDraft(
    val id: Long = 0,
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val amount: Double get() = quantity * unitPrice
}

data class CreateInvoiceUiState(
    val invoiceNumber: String = "",
    val selectedClient: ClientEntity? = null,
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (14 * 86_400_000L),
    val items: List<LineItemDraft> = listOf(LineItemDraft(description = "", quantity = 1.0, unitPrice = 0.0)),
    val taxPercent: Double = 0.0,
    val discountPercent: Double = 0.0,
    val notes: String = "Thank you for your business! Payment is appreciated by the due date.",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isLimitReached: Boolean = false,
    val savedInvoiceId: Long? = null,
    val clientError: String? = null,
    val dueDateError: String? = null,
    val itemDescriptionErrors: Set<Int> = emptySet(),
    val itemPriceErrors: Set<Int> = emptySet(),
    val amountError: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.amount }
    val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
    val taxAmount: Double get() = (subtotal - discountAmount) * (taxPercent / 100.0)
    val totalAmount: Double get() = maxOf(0.0, subtotal - discountAmount + taxAmount)
}

data class InvoiceValidationResult(
    val isValid: Boolean,
    val clientError: String? = null,
    val dueDateError: String? = null,
    val itemDescriptionErrors: Set<Int> = emptySet(),
    val itemPriceErrors: Set<Int> = emptySet(),
    val amountError: String? = null,
    val generalErrorMessage: String? = null
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.reminderLogDao())
    private val clientRepo = ClientRepository(db.clientDao())
    private val settingsRepo = PaymentSettingsRepository(db.paymentSettingsDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter = _statusFilter.asStateFlow()

    val clientsList: StateFlow<List<ClientEntity>> = clientRepo.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<PaymentSettingsEntity> = settingsRepo.settings
        .combine(MutableStateFlow(PaymentSettingsEntity())) { s, fallback -> s ?: fallback }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentSettingsEntity())

    val listUiState: StateFlow<InvoiceListUiState> = combine(
        invoiceRepo.allInvoicesWithDetails,
        _searchQuery,
        _statusFilter,
        settingsRepo.settings
    ) { invoices, query, filter, settings ->
        val safeSettings = settings ?: PaymentSettingsEntity()

        val filtered = invoices.filter { item ->
            val inv = item.invoice
            val clientName = item.client?.name ?: ""
            val matchesSearch = query.isBlank() ||
                    inv.invoiceNumber.contains(query, ignoreCase = true) ||
                    clientName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "PENDING" -> inv.status == "PENDING"
                "OVERDUE" -> inv.status == "OVERDUE"
                "PAID" -> inv.status == "PAID"
                else -> true
            }

            matchesSearch && matchesFilter
        }

        InvoiceListUiState(
            invoices = invoices,
            filteredInvoices = filtered,
            selectedFilter = filter,
            searchQuery = query,
            settings = safeSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceListUiState()
    )

    private val _createState = MutableStateFlow(CreateInvoiceUiState())
    val createState = _createState.asStateFlow()

    init {
        viewModelScope.launch {
            invoiceRepo.autoUpdateOverdueStatuses()
        }
    }

    fun initNewInvoice() {
        viewModelScope.launch {
            val nextNumber = invoiceRepo.generateNextInvoiceNumber()
            val settings = settingsRepo.getSettingsSync()
            val totalInvoices = db.invoiceDao().getTotalInvoiceCount()
            val isLimitReached = !settings.isProTier && totalInvoices >= settings.freeInvoicesLimit

            _createState.value = CreateInvoiceUiState(
                invoiceNumber = nextNumber,
                selectedClient = null,
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + (14 * 86_400_000L),
                items = listOf(LineItemDraft(description = "Professional Services", quantity = 1.0, unitPrice = 0.0)),
                notes = settings.defaultPaymentTerms.ifBlank { "Payment due within 14 days of issue." },
                isLimitReached = isLimitReached
            )
        }
    }

    fun setCreateClient(client: ClientEntity) {
        _createState.value = _createState.value.copy(
            selectedClient = client,
            clientError = null,
            error = if (_createState.value.clientError != null) null else _createState.value.error
        )
    }

    fun setDueDatePreset(daysAhead: Int) {
        val newDue = System.currentTimeMillis() + (daysAhead * 86_400_000L)
        _createState.value = _createState.value.copy(
            dueDate = newDue,
            dueDateError = null,
            error = if (_createState.value.dueDateError != null) null else _createState.value.error
        )
    }

    fun setCustomDueDate(timestamp: Long) {
        _createState.value = _createState.value.copy(
            dueDate = timestamp,
            dueDateError = null,
            error = if (_createState.value.dueDateError != null) null else _createState.value.error
        )
    }

    fun updateTaxPercent(tax: Double) {
        _createState.value = _createState.value.copy(taxPercent = tax)
    }

    fun updateDiscountPercent(discount: Double) {
        _createState.value = _createState.value.copy(discountPercent = discount)
    }

    fun updateNotes(notes: String) {
        _createState.value = _createState.value.copy(notes = notes)
    }

    fun addLineItem(description: String = "", qty: Double = 1.0, price: Double = 0.0) {
        val current = _createState.value.items.toMutableList()
        current.add(LineItemDraft(description = description, quantity = qty, unitPrice = price))
        _createState.value = _createState.value.copy(items = current)
    }

    fun updateLineItem(index: Int, description: String, qty: Double, price: Double) {
        val current = _createState.value.items.toMutableList()
        if (index in current.indices) {
            current[index] = LineItemDraft(
                description = description,
                quantity = qty,
                unitPrice = price
            )
            val descErrors = _createState.value.itemDescriptionErrors.toMutableSet()
            if (description.isNotBlank()) descErrors.remove(index)

            val priceErrors = _createState.value.itemPriceErrors.toMutableSet()
            if (qty > 0.0 && price > 0.0) priceErrors.remove(index)

            _createState.value = _createState.value.copy(
                items = current,
                itemDescriptionErrors = descErrors,
                itemPriceErrors = priceErrors,
                amountError = if (_createState.value.totalAmount > 0.0) null else _createState.value.amountError,
                error = if (descErrors.isEmpty() && priceErrors.isEmpty() && _createState.value.clientError == null) null else _createState.value.error
            )
        }
    }

    fun removeLineItem(index: Int) {
        val current = _createState.value.items.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            val descErrors = _createState.value.itemDescriptionErrors.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
            val priceErrors = _createState.value.itemPriceErrors.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
            _createState.value = _createState.value.copy(
                items = current,
                itemDescriptionErrors = descErrors,
                itemPriceErrors = priceErrors
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _statusFilter.value = filter
    }

    /**
     * Validates that all required fields are satisfied:
     * 1. A client is selected.
     * 2. Due date is valid and provided.
     * 3. Line items have valid descriptions and quantities.
     * 4. Amounts are strictly greater than zero.
     */
    fun validateForm(): InvoiceValidationResult {
        val state = _createState.value

        var clientErr: String? = null
        var dueDateErr: String? = null
        var amountErr: String? = null
        val descErrors = mutableSetOf<Int>()
        val priceErrors = mutableSetOf<Int>()
        val errorList = mutableListOf<String>()

        // 1. Client validation
        if (state.selectedClient == null) {
            clientErr = "Please select or add a client"
            errorList.add(clientErr)
        }

        // 2. Due date validation
        if (state.dueDate <= 0L) {
            dueDateErr = "Payment due date is required"
            errorList.add(dueDateErr)
        } else if (state.dueDate < state.issueDate - 86_400_000L) {
            dueDateErr = "Due date cannot be before invoice issue date"
            errorList.add(dueDateErr)
        }

        // 3. Item Description & Line Amount validation
        if (state.items.isEmpty()) {
            errorList.add("At least one line item is required")
        } else {
            state.items.forEachIndexed { index, item ->
                if (item.description.trim().isBlank()) {
                    descErrors.add(index)
                    errorList.add("Item description is required for item #${index + 1}")
                }
                if (item.quantity <= 0.0) {
                    priceErrors.add(index)
                    errorList.add("Quantity must be greater than 0 for item #${index + 1}")
                }
                if (item.unitPrice <= 0.0) {
                    priceErrors.add(index)
                    errorList.add("Unit price must be greater than 0 for item #${index + 1}")
                }
            }
        }

        // 4. Total Amount validation
        if (state.totalAmount <= 0.0) {
            amountErr = "Total invoice amount must be greater than zero"
            errorList.add(amountErr)
        }

        val isValid = errorList.isEmpty()
        val generalMsg = if (errorList.isNotEmpty()) errorList.first() else null

        return InvoiceValidationResult(
            isValid = isValid,
            clientError = clientErr,
            dueDateError = dueDateErr,
            itemDescriptionErrors = descErrors,
            itemPriceErrors = priceErrors,
            amountError = amountErr,
            generalErrorMessage = generalMsg
        )
    }

    fun saveInvoice(onSuccess: (Long) -> Unit) {
        val validation = validateForm()
        if (!validation.isValid) {
            _createState.value = _createState.value.copy(
                error = validation.generalErrorMessage,
                clientError = validation.clientError,
                dueDateError = validation.dueDateError,
                itemDescriptionErrors = validation.itemDescriptionErrors,
                itemPriceErrors = validation.itemPriceErrors,
                amountError = validation.amountError
            )
            return
        }

        val state = _createState.value
        viewModelScope.launch {
            _createState.value = state.copy(
                isSaving = true,
                error = null,
                clientError = null,
                dueDateError = null,
                itemDescriptionErrors = emptySet(),
                itemPriceErrors = emptySet(),
                amountError = null
            )
            try {
                val invoiceEntity = InvoiceEntity(
                    invoiceNumber = state.invoiceNumber,
                    clientId = state.selectedClient!!.id,
                    issueDate = state.issueDate,
                    dueDate = state.dueDate,
                    subtotal = state.subtotal,
                    taxPercent = state.taxPercent,
                    discountPercent = state.discountPercent,
                    totalAmount = state.totalAmount,
                    status = "PENDING",
                    notes = state.notes
                )
                val itemEntities = state.items.map {
                    InvoiceItemEntity(
                        invoiceId = 0,
                        description = it.description.trim(),
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        amount = it.amount
                    )
                }
                val newId = invoiceRepo.createInvoice(invoiceEntity, itemEntities)
                _createState.value = _createState.value.copy(isSaving = false, savedInvoiceId = newId)
                onSuccess(newId)
            } catch (e: Exception) {
                _createState.value = _createState.value.copy(
                    isSaving = false,
                    error = e.localizedMessage ?: "Failed to save invoice"
                )
            }
        }
    }

    fun getInvoiceDetails(id: Long) = invoiceRepo.getInvoiceWithDetailsById(id)

    fun markPaid(invoiceId: Long, paymentMethod: String) {
        viewModelScope.launch {
            invoiceRepo.markPaid(invoiceId, paymentMethod)
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            invoiceRepo.deleteInvoice(invoice)
        }
    }

    fun logReminderSent(
        invoiceWithDetails: InvoiceWithDetails,
        channel: String,
        reminderType: String,
        messageText: String
    ) {
        viewModelScope.launch {
            val invoice = invoiceWithDetails.invoice
            val client = invoiceWithDetails.client
            invoiceRepo.logReminder(
                ReminderLogEntity(
                    invoiceId = invoice.id,
                    clientName = client?.name ?: "Client",
                    invoiceNumber = invoice.invoiceNumber,
                    amount = invoice.totalAmount,
                    reminderType = reminderType,
                    channel = channel,
                    messageText = messageText
                )
            )
            // Update flags
            when (reminderType) {
                "BEFORE_DUE" -> invoiceRepo.updateReminderFlags(invoice.id, beforeDue = true, onDue = invoice.reminderOnDueSent, late3 = invoice.reminder3DaysLateSent, late7 = invoice.reminder7DaysLateSent)
                "ON_DUE" -> invoiceRepo.updateReminderFlags(invoice.id, beforeDue = invoice.reminderBeforeDueSent, onDue = true, late3 = invoice.reminder3DaysLateSent, late7 = invoice.reminder7DaysLateSent)
                "LATE_3_DAYS" -> invoiceRepo.updateReminderFlags(invoice.id, beforeDue = invoice.reminderBeforeDueSent, onDue = invoice.reminderOnDueSent, late3 = true, late7 = invoice.reminder7DaysLateSent)
                "LATE_7_DAYS" -> invoiceRepo.updateReminderFlags(invoice.id, beforeDue = invoice.reminderBeforeDueSent, onDue = invoice.reminderOnDueSent, late3 = invoice.reminder3DaysLateSent, late7 = true)
            }
        }
    }

    fun generatePdf(invoiceWithDetails: InvoiceWithDetails): Uri? {
        val settings = settingsState.value
        return PdfInvoiceGenerator.generatePdf(
            context = getApplication(),
            invoiceWithDetails = invoiceWithDetails,
            settings = settings
        )
    }
}
