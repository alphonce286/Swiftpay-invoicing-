package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val clientName: String,
    val invoiceNumber: String,
    val amount: Double,
    val reminderType: String, // UPCOMING, DUE_TODAY, LATE_3_DAYS, LATE_7_DAYS, CUSTOM
    val channel: String, // WHATSAPP, SMS, EMAIL
    val messageText: String,
    val sentAt: Long = System.currentTimeMillis()
)
