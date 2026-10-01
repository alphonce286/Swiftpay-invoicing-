package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.PaymentSettingsRepository
import com.example.model.InvoiceWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val paidTotal: Double = 0.0,
    val paidCount: Int = 0,
    val pendingTotal: Double = 0.0,
    val pendingCount: Int = 0,
    val overdueTotal: Double = 0.0,
    val overdueCount: Int = 0,
    val recentInvoices: List<InvoiceWithDetails> = emptyList(),
    val urgentOverdueInvoices: List<InvoiceWithDetails> = emptyList(),
    val settings: PaymentSettingsEntity = PaymentSettingsEntity(),
    val totalInvoicesCount: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.reminderLogDao())
    private val settingsRepo = PaymentSettingsRepository(db.paymentSettingsDao())

    init {
        viewModelScope.launch {
            invoiceRepo.autoUpdateOverdueStatuses()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        invoiceRepo.allInvoicesWithDetails,
        settingsRepo.settings
    ) { invoices, settingsEntity ->
        val safeSettings = settingsEntity ?: PaymentSettingsEntity()

        var paidTot = 0.0
        var paidCnt = 0
        var pendingTot = 0.0
        var pendingCnt = 0
        var overdueTot = 0.0
        var overdueCnt = 0

        for (item in invoices) {
            val inv = item.invoice
            when (inv.status.uppercase()) {
                "PAID" -> {
                    paidTot += inv.totalAmount
                    paidCnt++
                }
                "OVERDUE" -> {
                    overdueTot += inv.totalAmount
                    overdueCnt++
                }
                else -> {
                    // PENDING or DRAFT
                    pendingTot += inv.totalAmount
                    pendingCnt++
                }
            }
        }

        val overdues = invoices.filter { it.invoice.status == "OVERDUE" }

        DashboardUiState(
            paidTotal = paidTot,
            paidCount = paidCnt,
            pendingTotal = pendingTot,
            pendingCount = pendingCnt,
            overdueTotal = overdueTot,
            overdueCount = overdueCnt,
            recentInvoices = invoices.take(5),
            urgentOverdueInvoices = overdues,
            settings = safeSettings,
            totalInvoicesCount = invoices.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun markInvoicePaid(invoiceId: Long, paymentMethod: String) {
        viewModelScope.launch {
            invoiceRepo.markPaid(invoiceId, paymentMethod)
        }
    }
}
