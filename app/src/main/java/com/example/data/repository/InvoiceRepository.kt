package com.example.data.repository

import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.ReminderLogDao
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ReminderLogEntity
import com.example.model.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val reminderLogDao: ReminderLogDao
) {

    val allInvoicesWithDetails: Flow<List<InvoiceWithDetails>> =
        invoiceDao.getAllInvoicesWithDetails()

    val allReminderLogs: Flow<List<ReminderLogEntity>> =
        reminderLogDao.getAllReminderLogs()

    fun getInvoiceWithDetailsById(id: Long): Flow<InvoiceWithDetails?> =
        invoiceDao.getInvoiceWithDetailsById(id)

    fun getInvoicesForClient(clientId: Long): Flow<List<InvoiceWithDetails>> =
        invoiceDao.getInvoicesForClient(clientId)

    suspend fun createInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertInvoiceItems(itemsWithId)
        return invoiceId
    }

    suspend fun updateInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ) {
        invoiceDao.updateInvoice(invoice)
        invoiceDao.deleteItemsForInvoice(invoice.id)
        val itemsWithId = items.map { it.copy(invoiceId = invoice.id) }
        invoiceDao.insertInvoiceItems(itemsWithId)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        invoiceDao.deleteItemsForInvoice(invoice.id)
        invoiceDao.deleteInvoice(invoice)
    }

    suspend fun markPaid(
        invoiceId: Long,
        paymentMethod: String,
        paidDate: Long = System.currentTimeMillis()
    ) {
        invoiceDao.markInvoicePaid(
            id = invoiceId,
            status = "PAID",
            paidDate = paidDate,
            paymentMethod = paymentMethod
        )
    }

    suspend fun markStatus(invoiceId: Long, status: String) {
        invoiceDao.updateStatus(invoiceId, status)
    }

    suspend fun logReminder(log: ReminderLogEntity) {
        reminderLogDao.insertReminderLog(log)
    }

    suspend fun updateReminderFlags(
        invoiceId: Long,
        beforeDue: Boolean,
        onDue: Boolean,
        late3: Boolean,
        late7: Boolean
    ) {
        invoiceDao.updateReminderFlags(invoiceId, beforeDue, onDue, late3, late7)
    }

    suspend fun generateNextInvoiceNumber(): String {
        val totalCount = invoiceDao.getTotalInvoiceCount()
        return "INV-${1001 + totalCount}"
    }

    suspend fun autoUpdateOverdueStatuses() {
        val now = System.currentTimeMillis()
        val all = invoiceDao.getAllInvoicesWithDetails().firstOrNull() ?: return
        for (item in all) {
            val inv = item.invoice
            if (inv.status == "PENDING" && inv.dueDate < now) {
                invoiceDao.updateStatus(inv.id, "OVERDUE")
            }
        }
    }
}
