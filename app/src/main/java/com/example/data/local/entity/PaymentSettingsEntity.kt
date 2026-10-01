package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_settings")
data class PaymentSettingsEntity(
    @PrimaryKey
    val id: Long = 1L,
    val businessName: String = "My Freelance Studio",
    val businessEmail: String = "billing@mystudio.com",
    val businessPhone: String = "+1 (555) 019-2834",
    val businessAddress: String = "Suite 400, Innovation Hub",
    val currencySymbol: String = "$",
    val mobileMoneyProvider: String = "M-Pesa / Till",
    val mobileMoneyNumber: String = "Till: 892019",
    val onlinePaymentUrl: String = "https://pay.stripe.com/p/invoiceflow_demo",
    val bankName: String = "First International Bank",
    val bankAccountNumber: String = "9876543210",
    val bankAccountName: String = "My Freelance Studio LLC",
    val defaultPaymentTerms: String = "Payment due within 14 days of issue.",
    val isProTier: Boolean = false,
    val freeInvoicesLimit: Int = 3
)
