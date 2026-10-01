package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.model.InvoiceWithDetails
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceGenerator {

    fun generatePdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        settings: PaymentSettingsEntity
    ): Uri? {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val items = invoiceWithDetails.items

        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(Color.WHITE)

        // Top Header Banner
        paint.color = Color.parseColor("#0F172A") // Deep Navy
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Business Name
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(settings.businessName.ifEmpty { "InvoiceFlow Business" }, 36f, 42f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#CBD5E1")
        val contactLine = listOfNotNull(
            settings.businessEmail.takeIf { it.isNotBlank() },
            settings.businessPhone.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        canvas.drawText(contactLine, 36f, 60f, paint)
        if (settings.businessAddress.isNotBlank()) {
            canvas.drawText(settings.businessAddress, 36f, 74f, paint)
        }

        // INVOICE text on right header
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", (pageWidth - 36).toFloat(), 44f, paint)

        paint.textSize = 10f
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawText(invoice.invoiceNumber, (pageWidth - 36).toFloat(), 62f, paint)

        // Reset text align
        paint.textAlign = Paint.Align.LEFT

        // Status Badge below header
        val statusBgColor = when (invoice.status) {
            "PAID" -> Color.parseColor("#D1FAE5")
            "OVERDUE" -> Color.parseColor("#FEE2E2")
            else -> Color.parseColor("#FEF3C7")
        }
        val statusTextColor = when (invoice.status) {
            "PAID" -> Color.parseColor("#065F46")
            "OVERDUE" -> Color.parseColor("#991B1B")
            else -> Color.parseColor("#92400E")
        }

        val badgeRect = RectF((pageWidth - 130).toFloat(), 110f, (pageWidth - 36).toFloat(), 132f)
        paint.color = statusBgColor
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.color = statusTextColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(invoice.status, badgeRect.centerX(), 125f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Meta info (Issue Date / Due Date)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Issue Date:", (pageWidth - 170).toFloat(), 150f, paint)
        canvas.drawText("Due Date:", (pageWidth - 170).toFloat(), 165f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(CurrencyFormatter.formatDate(invoice.issueDate), (pageWidth - 110).toFloat(), 150f, paint)
        canvas.drawText(CurrencyFormatter.formatDate(invoice.dueDate), (pageWidth - 110).toFloat(), 165f, paint)

        // Bill To section
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO:", 36f, 120f, paint)

        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(client?.name ?: "Valued Client", 36f, 138f, paint)

        paint.textSize = 9.5f
        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        var clientY = 152f
        if (!client?.company.isNullOrBlank()) {
            canvas.drawText(client?.company ?: "", 36f, clientY, paint)
            clientY += 14f
        }
        if (!client?.email.isNullOrBlank()) {
            canvas.drawText(client?.email ?: "", 36f, clientY, paint)
            clientY += 14f
        }
        if (!client?.phone.isNullOrBlank()) {
            canvas.drawText(client?.phone ?: "", 36f, clientY, paint)
            clientY += 14f
        }
        if (!client?.address.isNullOrBlank()) {
            canvas.drawText(client?.address ?: "", 36f, clientY, paint)
            clientY += 14f
        }

        // Table Header
        val tableTop = maxOf(clientY + 15f, 195f)
        val tableBottom = tableTop + 24f

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, tableTop, (pageWidth - 36).toFloat(), tableBottom, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("DESCRIPTION", 46f, tableTop + 16f, paint)
        canvas.drawText("QTY", 320f, tableTop + 16f, paint)
        canvas.drawText("RATE", 390f, tableTop + 16f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", (pageWidth - 46).toFloat(), tableTop + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Items Rows
        var itemY = tableBottom + 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for ((index, item) in items.withIndex()) {
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(36f, itemY - 14f, (pageWidth - 36).toFloat(), itemY + 8f, paint)
            }

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9.5f
            canvas.drawText(item.description, 46f, itemY, paint)
            canvas.drawText("${item.quantity.toInt()}", 325f, itemY, paint)
            canvas.drawText(CurrencyFormatter.format(item.unitPrice, settings.currencySymbol), 390f, itemY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyFormatter.format(item.amount, settings.currencySymbol), (pageWidth - 46).toFloat(), itemY, paint)
            paint.textAlign = Paint.Align.LEFT

            itemY += 24f
        }

        // Horizontal line under table
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, itemY, (pageWidth - 36).toFloat(), itemY, paint)
        paint.strokeWidth = 0f

        // Totals Box (Right aligned)
        val totalsX = (pageWidth - 220).toFloat()
        var totalsY = itemY + 18f

        paint.textSize = 9.5f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Subtotal:", totalsX, totalsY, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText(CurrencyFormatter.format(invoice.subtotal, settings.currencySymbol), (pageWidth - 46).toFloat(), totalsY, paint)
        paint.textAlign = Paint.Align.LEFT

        if (invoice.discountPercent > 0) {
            totalsY += 16f
            paint.color = Color.parseColor("#059669")
            canvas.drawText("Discount (${invoice.discountPercent}%):", totalsX, totalsY, paint)
            paint.textAlign = Paint.Align.RIGHT
            val discVal = invoice.subtotal * (invoice.discountPercent / 100.0)
            canvas.drawText("-${CurrencyFormatter.format(discVal, settings.currencySymbol)}", (pageWidth - 46).toFloat(), totalsY, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        if (invoice.taxPercent > 0) {
            totalsY += 16f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Tax (${invoice.taxPercent}%):", totalsX, totalsY, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#0F172A")
            val taxVal = invoice.subtotal * (invoice.taxPercent / 100.0)
            canvas.drawText("+${CurrencyFormatter.format(taxVal, settings.currencySymbol)}", (pageWidth - 46).toFloat(), totalsY, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // Total Due Highlight
        totalsY += 24f
        val totalHighlightRect = RectF(totalsX - 10f, totalsY - 14f, (pageWidth - 36).toFloat(), totalsY + 14f)
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(totalHighlightRect, 6f, 6f, paint)

        paint.color = Color.parseColor("#1E40AF")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL DUE:", totalsX, totalsY + 4f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyFormatter.format(invoice.totalAmount, settings.currencySymbol), (pageWidth - 46).toFloat(), totalsY + 4f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Payment Methods & Notes Section (Left side below items)
        var paymentY = itemY + 22f

        val paymentBox = RectF(36f, paymentY - 8f, totalsX - 25f, (pageHeight - 65).toFloat())
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(paymentBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HOW TO PAY", 48f, paymentY + 8f, paint)
        paymentY += 22f

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#334155")

        if (settings.onlinePaymentUrl.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Card / Online Link:", 48f, paymentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#2563EB")
            canvas.drawText(settings.onlinePaymentUrl.take(45), 48f, paymentY + 12f, paint)
            paint.color = Color.parseColor("#334155")
            paymentY += 24f
        }

        if (settings.mobileMoneyNumber.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${settings.mobileMoneyProvider}:", 48f, paymentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(settings.mobileMoneyNumber, 48f, paymentY + 12f, paint)
            paymentY += 24f
        }

        if (settings.bankAccountNumber.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Bank Transfer:", 48f, paymentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${settings.bankName}", 48f, paymentY + 12f, paint)
            canvas.drawText("Acc: ${settings.bankAccountNumber} (${settings.bankAccountName})", 48f, paymentY + 24f, paint)
            paymentY += 36f
        }

        if (invoice.notes.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Terms & Notes:", 48f, paymentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val noteWords = invoice.notes.take(90)
            canvas.drawText(noteWords, 48f, paymentY + 12f, paint)
        }

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Generated with InvoiceFlow • Thank you for your business!",
            (pageWidth / 2).toFloat(),
            (pageHeight - 25).toFloat(),
            paint
        )

        document.finishPage(page)

        return try {
            val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val cleanInvNum = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "")
            val pdfFile = File(invoicesDir, "Invoice_$cleanInvNum.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }
}
