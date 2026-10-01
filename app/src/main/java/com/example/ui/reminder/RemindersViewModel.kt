package com.example.ui.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.data.local.entity.ReminderLogEntity
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.PaymentSettingsRepository
import com.example.model.InvoiceWithDetails
import com.example.util.ReminderTiming
import com.example.util.ReminderTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InvoiceReminderStatus(
    val invoiceWithDetails: InvoiceWithDetails,
    val suggestedTiming: ReminderTiming,
    val daysDifference: Int,
    val isDueToday: Boolean,
    val isOverdue: Boolean
)

data class RemindersUiState(
    val pendingReminders: List<InvoiceReminderStatus> = emptyList(),
    val reminderLogs: List<ReminderLogEntity> = emptyList(),
    val settings: PaymentSettingsEntity = PaymentSettingsEntity(),
    val totalPendingRemindersCount: Int = 0
)

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.reminderLogDao())
    private val settingsRepo = PaymentSettingsRepository(db.paymentSettingsDao())

    val uiState: StateFlow<RemindersUiState> = combine(
        invoiceRepo.allInvoicesWithDetails,
        invoiceRepo.allReminderLogs,
        settingsRepo.settings
    ) { invoices, logs, settings ->
        val safeSettings = settings ?: PaymentSettingsEntity()
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val list = mutableListOf<InvoiceReminderStatus>()

        for (item in invoices) {
            val inv = item.invoice
            if (inv.status == "PAID") continue

            val diffMs = inv.dueDate - now
            val diffDays = (diffMs / dayMs).toInt()

            val (timing, needed) = when {
                diffDays < -6 && !inv.reminder7DaysLateSent -> Pair(ReminderTiming.LATE_7_DAYS, true)
                diffDays in -6..-2 && !inv.reminder3DaysLateSent -> Pair(ReminderTiming.LATE_3_DAYS, true)
                diffDays in -1..0 && !inv.reminderOnDueSent -> Pair(ReminderTiming.ON_DUE, true)
                diffDays in 1..3 && !inv.reminderBeforeDueSent -> Pair(ReminderTiming.BEFORE_DUE, true)
                else -> {
                    // Fallback reminder suggestion based on timeline even if sent before
                    if (diffDays < 0) Pair(ReminderTiming.LATE_3_DAYS, false)
                    else Pair(ReminderTiming.BEFORE_DUE, false)
                }
            }

            list.add(
                InvoiceReminderStatus(
                    invoiceWithDetails = item,
                    suggestedTiming = timing,
                    daysDifference = diffDays,
                    isDueToday = diffDays == 0,
                    isOverdue = diffDays < 0
                )
            )
        }

        // Sort by urgency: overdue first, then due today, then upcoming
        val sorted = list.sortedBy { it.daysDifference }

        RemindersUiState(
            pendingReminders = sorted,
            reminderLogs = logs,
            settings = safeSettings,
            totalPendingRemindersCount = sorted.count { it.isOverdue || it.isDueToday }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState()
    )

    fun logSentReminder(
        invoiceWithDetails: InvoiceWithDetails,
        channel: String,
        timing: ReminderTiming,
        message: String
    ) {
        viewModelScope.launch {
            val inv = invoiceWithDetails.invoice
            val client = invoiceWithDetails.client
            invoiceRepo.logReminder(
                ReminderLogEntity(
                    invoiceId = inv.id,
                    clientName = client?.name ?: "Client",
                    invoiceNumber = inv.invoiceNumber,
                    amount = inv.totalAmount,
                    reminderType = timing.name,
                    channel = channel,
                    messageText = message
                )
            )
            when (timing) {
                ReminderTiming.BEFORE_DUE -> invoiceRepo.updateReminderFlags(inv.id, true, inv.reminderOnDueSent, inv.reminder3DaysLateSent, inv.reminder7DaysLateSent)
                ReminderTiming.ON_DUE -> invoiceRepo.updateReminderFlags(inv.id, inv.reminderBeforeDueSent, true, inv.reminder3DaysLateSent, inv.reminder7DaysLateSent)
                ReminderTiming.LATE_3_DAYS -> invoiceRepo.updateReminderFlags(inv.id, inv.reminderBeforeDueSent, inv.reminderOnDueSent, true, inv.reminder7DaysLateSent)
                ReminderTiming.LATE_7_DAYS -> invoiceRepo.updateReminderFlags(inv.id, inv.reminderBeforeDueSent, inv.reminderOnDueSent, inv.reminder3DaysLateSent, true)
            }
        }
    }
}
