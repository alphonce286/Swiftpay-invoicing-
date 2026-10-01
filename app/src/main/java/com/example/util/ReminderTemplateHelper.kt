package com.example.util

import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.model.InvoiceWithDetails

enum class ReminderTone {
    FRIENDLY,
    PROFESSIONAL,
    URGENT
}

enum class ReminderTiming(val label: String) {
    BEFORE_DUE("Before Due Date (Upcoming)"),
    ON_DUE("On Due Date (Today)"),
    LATE_3_DAYS("3 Days Overdue"),
    LATE_7_DAYS("7 Days Overdue (Final Notice)")
}

object ReminderTemplateHelper {

    fun generateMessage(
        invoiceWithDetails: InvoiceWithDetails,
        settings: PaymentSettingsEntity,
        timing: ReminderTiming,
        tone: ReminderTone = ReminderTone.PROFESSIONAL
    ): String {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val clientName = client?.name ?: "Client"
        val businessName = settings.businessName.ifEmpty { "Our Business" }
        val amountStr = CurrencyFormatter.format(invoice.totalAmount, settings.currencySymbol)
        val dueDateStr = CurrencyFormatter.formatDate(invoice.dueDate)

        val paymentDetails = buildString {
            if (settings.onlinePaymentUrl.isNotBlank()) {
                append("\n💳 Pay Online: ${settings.onlinePaymentUrl}")
            }
            if (settings.mobileMoneyNumber.isNotBlank()) {
                append("\n📱 ${settings.mobileMoneyProvider}: ${settings.mobileMoneyNumber}")
            }
            if (settings.bankAccountNumber.isNotBlank()) {
                append("\n🏦 Bank Transfer: ${settings.bankName}, Acc: ${settings.bankAccountNumber} (${settings.bankAccountName})")
            }
        }

        return when (timing) {
            ReminderTiming.BEFORE_DUE -> {
                when (tone) {
                    ReminderTone.FRIENDLY -> """
                        Hi $clientName! 😊
                        Just a friendly reminder that invoice *${invoice.invoiceNumber}* for *$amountStr* from *$businessName* is due on *$dueDateStr*.
                        $paymentDetails
                        
                        Thank you for working with us! Please let us know if you have any questions.
                    """.trimIndent()

                    ReminderTone.PROFESSIONAL -> """
                        Hello $clientName,
                        This is a courtesy reminder regarding upcoming invoice *${invoice.invoiceNumber}* for *$amountStr*, due on *$dueDateStr*.
                        $paymentDetails
                        
                        Thank you for your prompt attention to this invoice.
                        Best regards,
                        $businessName
                    """.trimIndent()

                    ReminderTone.URGENT -> """
                        Attention: $clientName,
                        Please be reminded that invoice *${invoice.invoiceNumber}* for *$amountStr* is due soon on *$dueDateStr*.
                        $paymentDetails
                        
                        Regards,
                        $businessName
                    """.trimIndent()
                }
            }

            ReminderTiming.ON_DUE -> {
                """
                    Hello $clientName,
                    Invoice *${invoice.invoiceNumber}* for *$amountStr* from *$businessName* is *due today* ($dueDateStr).
                    $paymentDetails
                    
                    Kindly process payment today to keep your account in good standing. If payment has already been sent, please disregard this note.
                    
                    Thank you!
                    $businessName
                """.trimIndent()
            }

            ReminderTiming.LATE_3_DAYS -> {
                """
                    Hi $clientName,
                    We haven't received payment yet for invoice *${invoice.invoiceNumber}* ($amountStr), which was due on *$dueDateStr* (now 3 days past due).
                    $paymentDetails
                    
                    Please arrange for payment at your earliest convenience or reach out if there is an issue with the invoice.
                    
                    Sincerely,
                    $businessName
                """.trimIndent()
            }

            ReminderTiming.LATE_7_DAYS -> {
                """
                    URGENT: Outstanding Payment Notice
                    Dear $clientName,
                    
                    Invoice *${invoice.invoiceNumber}* for *$amountStr* is now *7 days overdue* (was due on $dueDateStr).
                    
                    Please settle this outstanding balance immediately via:
                    $paymentDetails
                    
                    If you are experiencing any difficulty or have already transferred the funds, please reply with the payment confirmation details immediately.
                    
                    Thank you for your cooperation,
                    $businessName
                """.trimIndent()
            }
        }
    }

    fun generateQuickShareText(
        invoiceWithDetails: InvoiceWithDetails,
        settings: PaymentSettingsEntity
    ): String {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val clientName = client?.name ?: "Client"
        val businessName = settings.businessName.ifEmpty { "Our Business" }
        val amountStr = CurrencyFormatter.format(invoice.totalAmount, settings.currencySymbol)
        val dueDateStr = CurrencyFormatter.formatDate(invoice.dueDate)

        val paymentDetails = buildString {
            if (settings.onlinePaymentUrl.isNotBlank()) {
                append("\n💳 Pay online: ${settings.onlinePaymentUrl}")
            }
            if (settings.mobileMoneyNumber.isNotBlank()) {
                append("\n📱 ${settings.mobileMoneyProvider}: ${settings.mobileMoneyNumber}")
            }
            if (settings.bankAccountNumber.isNotBlank()) {
                append("\n🏦 Bank Transfer: ${settings.bankName} - ${settings.bankAccountNumber}")
            }
        }

        val itemsSummary = invoiceWithDetails.items.joinToString("\n") {
            " • ${it.description} (${it.quantity.toInt()}x ${CurrencyFormatter.format(it.unitPrice, settings.currencySymbol)})"
        }

        return """
            📄 *INVOICE ${invoice.invoiceNumber}*
            From: $businessName
            To: $clientName
            Due Date: $dueDateStr
            Total Due: *$amountStr*
            
            *Items:*
            $itemsSummary
            $paymentDetails
            
            Thank you for your business!
        """.trimIndent()
    }
}
