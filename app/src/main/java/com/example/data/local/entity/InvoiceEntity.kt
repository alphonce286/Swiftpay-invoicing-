package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("clientId")]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val clientId: Long,
    val issueDate: Long,
    val dueDate: Long,
    val subtotal: Double,
    val taxPercent: Double = 0.0,
    val discountPercent: Double = 0.0,
    val totalAmount: Double,
    val status: String = "PENDING", // PENDING, PAID, OVERDUE
    val paidDate: Long? = null,
    val paymentMethod: String = "",
    val paymentReference: String = "",
    val notes: String = "Thank you for your business! Payment is appreciated by the due date.",
    val reminderBeforeDueSent: Boolean = false,
    val reminderOnDueSent: Boolean = false,
    val reminder3DaysLateSent: Boolean = false,
    val reminder7DaysLateSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
