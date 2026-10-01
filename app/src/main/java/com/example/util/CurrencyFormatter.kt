package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "$"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$symbol${formatter.format(amount)}"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getDueDateLabel(dueDate: Long, status: String): Pair<String, Boolean> {
        if (status == "PAID") {
            return Pair("Paid", false)
        }
        val now = System.currentTimeMillis()
        val diffMs = dueDate - now
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> Pair("Overdue by ${-diffDays} day${if (-diffDays > 1) "s" else ""}", true)
            diffDays == 0 -> Pair("Due today", true)
            diffDays == 1 -> Pair("Due tomorrow", false)
            else -> Pair("Due in $diffDays days", false)
        }
    }
}
