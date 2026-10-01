package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.model.InvoiceWithDetails
import java.net.URLEncoder

data class InvoiceShareBundle(
    val deepLink: String,
    val subject: String,
    val summaryText: String,
    val whatsAppMessage: String,
    val smsBody: String,
    val emailBody: String
)

object SharingHelper {

    private const val DEEP_LINK_BASE = "https://invoiceflow.app/invoice"

    /**
     * Generates a web and in-app deep link for an invoice.
     * Handled by the intent-filter in AndroidManifest.xml:
     * https://invoiceflow.app/invoice/{invoiceId}
     */
    fun generateDeepLink(invoiceId: Long, invoiceNumber: String = ""): String {
        return "$DEEP_LINK_BASE/$invoiceId"
    }

    /**
     * Creates tailored share texts containing the deep link, invoice summary, and payment methods.
     */
    fun buildShareBundle(
        invoiceWithDetails: InvoiceWithDetails,
        settings: PaymentSettingsEntity
    ): InvoiceShareBundle {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val clientName = client?.name ?: "Valued Client"
        val deepLink = generateDeepLink(invoice.id, invoice.invoiceNumber)
        val formattedAmount = CurrencyFormatter.format(invoice.totalAmount, settings.currencySymbol)
        val dueDateStr = CurrencyFormatter.formatDate(invoice.dueDate)
        val businessName = settings.businessName.ifBlank { "InvoiceFlow" }

        val subject = "Invoice ${invoice.invoiceNumber} from $businessName ($formattedAmount)"

        val paymentDetails = StringBuilder()
        if (settings.onlinePaymentUrl.isNotBlank()) {
            paymentDetails.append("💳 Pay Online: ${settings.onlinePaymentUrl}\n")
        }
        if (settings.mobileMoneyNumber.isNotBlank()) {
            paymentDetails.append("📱 ${settings.mobileMoneyProvider}: ${settings.mobileMoneyNumber}\n")
        }
        if (settings.bankAccountNumber.isNotBlank()) {
            paymentDetails.append("🏦 Bank: ${settings.bankName}, Acc: ${settings.bankAccountNumber} (${settings.bankAccountName})\n")
        }

        val summaryText = buildString {
            append("📄 INVOICE ${invoice.invoiceNumber}\n")
            append("To: $clientName\n")
            append("Amount: $formattedAmount\n")
            append("Due Date: $dueDateStr\n")
            append("🔗 View & Download Invoice: $deepLink\n")
            if (paymentDetails.isNotBlank()) {
                append("\nPayment Instructions:\n$paymentDetails")
            }
        }

        val whatsAppMessage = buildString {
            append("Hello $clientName, here is your invoice from *$businessName*:\n\n")
            append("📄 *Invoice ${invoice.invoiceNumber}*\n")
            append("💰 *Amount Due:* $formattedAmount\n")
            append("📅 *Due Date:* $dueDateStr\n\n")
            append("🔗 *View & Pay Online:* $deepLink\n\n")
            if (paymentDetails.isNotBlank()) {
                append("*How to Pay:*\n$paymentDetails\n")
            }
            append("Thank you for your business!")
        }

        val smsBody = "Hi $clientName, your invoice ${invoice.invoiceNumber} for $formattedAmount is ready. Due by $dueDateStr. View & pay here: $deepLink"

        val emailBody = buildString {
            append("Dear $clientName,\n\n")
            append("Please find attached invoice ${invoice.invoiceNumber} from $businessName.\n\n")
            append("Summary:\n")
            append("• Invoice Number: ${invoice.invoiceNumber}\n")
            append("• Amount Due: $formattedAmount\n")
            append("• Due Date: $dueDateStr\n\n")
            append("You can review and track your invoice directly online via this deep link:\n")
            append("$deepLink\n\n")
            if (paymentDetails.isNotBlank()) {
                append("Payment Methods:\n$paymentDetails\n")
            }
            append("Thank you for your business!\n\n")
            append("Best regards,\n")
            append(businessName)
        }

        return InvoiceShareBundle(
            deepLink = deepLink,
            subject = subject,
            summaryText = summaryText,
            whatsAppMessage = whatsAppMessage,
            smsBody = smsBody,
            emailBody = emailBody
        )
    }

    /**
     * Share via WhatsApp using Intent (ACTION_VIEW with fallback to ACTION_SEND).
     */
    fun shareViaWhatsApp(context: Context, phone: String, messageText: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "").removePrefix("+")
            val encodedMsg = URLEncoder.encode(messageText, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Use ACTION_SEND with WhatsApp package or system chooser
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(sendIntent)
            } catch (ex: Exception) {
                shareViaChooser(context, "Share via WhatsApp", messageText)
            }
        }
    }

    /**
     * Share via SMS using Intent (ACTION_SENDTO with smsto: scheme).
     */
    fun shareViaSms(context: Context, phone: String, messageText: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaChooser(context, "Send SMS", messageText)
        }
    }

    /**
     * Share via Email using Intent (ACTION_SEND / ACTION_SENDTO with optional PDF attachment).
     */
    fun shareViaEmail(
        context: Context,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        pdfUri: Uri? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (pdfUri != null) "application/pdf" else "message/rfc822"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                if (pdfUri != null) {
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Send Email via..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share an official PDF file using Intent with FLAG_GRANT_READ_URI_PERMISSION.
     */
    fun sharePdfFile(context: Context, fileUri: Uri, invoiceNumber: String, deepLink: String = "") {
        try {
            val textBody = if (deepLink.isNotBlank()) {
                "Please find attached invoice $invoiceNumber.\n\nView online: $deepLink"
            } else {
                "Please find attached invoice $invoiceNumber."
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice $invoiceNumber")
                putExtra(Intent.EXTRA_TEXT, textBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generic Intent Chooser for sharing text or deep link to any app.
     */
    fun shareViaChooser(context: Context, title: String, text: String, pdfUri: Uri? = null) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (pdfUri != null) "application/pdf" else "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                if (pdfUri != null) {
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the invoice deep link to clipboard and notifies user.
     */
    fun copyDeepLink(context: Context, deepLink: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invoice Deep Link", deepLink)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Invoice deep link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Invoice Details") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
