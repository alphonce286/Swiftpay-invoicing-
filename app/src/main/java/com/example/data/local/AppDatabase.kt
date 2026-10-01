package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ClientDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.PaymentSettingsDao
import com.example.data.local.dao.ReminderLogDao
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.data.local.entity.ReminderLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ClientEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentSettingsEntity::class,
        ReminderLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentSettingsDao(): PaymentSettingsDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "invoice_flow.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }

            private suspend fun seedInitialData(database: AppDatabase) {
                val settingsDao = database.paymentSettingsDao()
                val clientDao = database.clientDao()
                val invoiceDao = database.invoiceDao()

                // Insert default business payment settings
                settingsDao.insertOrUpdate(
                    PaymentSettingsEntity(
                        id = 1L,
                        businessName = "Apex Creative Studio",
                        businessEmail = "billing@apexstudio.io",
                        businessPhone = "+1 (555) 234-5678",
                        businessAddress = "742 Evergreen Terrace, Suite 101",
                        currencySymbol = "$",
                        mobileMoneyProvider = "M-Pesa / Till",
                        mobileMoneyNumber = "Till No: 981240",
                        onlinePaymentUrl = "https://pay.stripe.com/p/invoiceflow_demo",
                        bankName = "Chase Business Banking",
                        bankAccountNumber = "48291048291",
                        bankAccountName = "Apex Creative Studio LLC",
                        defaultPaymentTerms = "Payment due within 14 days of invoice issue.",
                        isProTier = false,
                        freeInvoicesLimit = 3
                    )
                )

                // Seed starter clients for freelancers / shop owners
                val clientId1 = clientDao.insertClient(
                    ClientEntity(
                        name = "Sarah Jenkins",
                        company = "Bright Path Marketing",
                        phone = "+1 (555) 304-9120",
                        email = "sarah@brightpath.com",
                        address = "450 Market St, San Francisco, CA",
                        notes = "Preferred contact via WhatsApp for invoices"
                    )
                )

                val clientId2 = clientDao.insertClient(
                    ClientEntity(
                        name = "David Mwangi",
                        company = "Kijani Tech Solutions",
                        phone = "+254 712 345678",
                        email = "david@kijanitech.co.ke",
                        address = "Kilimani Business Center, Nairobi",
                        notes = "Pays primarily via M-Pesa / Bank Transfer"
                    )
                )

                val clientId3 = clientDao.insertClient(
                    ClientEntity(
                        name = "Elena Rostova",
                        company = "Nordic Design Lab",
                        phone = "+44 20 7946 0912",
                        email = "elena@nordiclab.eu",
                        address = "22 Baker Street, London",
                        notes = "Net 14 payment cycle"
                    )
                )

                val now = System.currentTimeMillis()
                val dayMs = 86_400_000L

                // 1. Pending invoice (Due in 3 days)
                val inv1Id = invoiceDao.insertInvoice(
                    InvoiceEntity(
                        invoiceNumber = "INV-1001",
                        clientId = clientId1,
                        issueDate = now - (4 * dayMs),
                        dueDate = now + (3 * dayMs),
                        subtotal = 850.0,
                        taxPercent = 5.0,
                        discountPercent = 0.0,
                        totalAmount = 892.50,
                        status = "PENDING",
                        notes = "Website redesign milestone 2: UI assets and responsive landing page."
                    )
                )
                invoiceDao.insertInvoiceItems(
                    listOf(
                        InvoiceItemEntity(
                            invoiceId = inv1Id,
                            description = "Responsive Landing Page UI Design",
                            quantity = 1.0,
                            unitPrice = 500.0,
                            amount = 500.0
                        ),
                        InvoiceItemEntity(
                            invoiceId = inv1Id,
                            description = "Mobile Component Library & Icons",
                            quantity = 1.0,
                            unitPrice = 350.0,
                            amount = 350.0
                        )
                    )
                )

                // 2. Overdue invoice (Overdue by 4 days) -> triggers reminder prompt!
                val inv2Id = invoiceDao.insertInvoice(
                    InvoiceEntity(
                        invoiceNumber = "INV-1002",
                        clientId = clientId2,
                        issueDate = now - (18 * dayMs),
                        dueDate = now - (4 * dayMs),
                        subtotal = 1200.0,
                        taxPercent = 0.0,
                        discountPercent = 0.0,
                        totalAmount = 1200.00,
                        status = "OVERDUE",
                        notes = "E-Commerce backend integration and payment gateway setup."
                    )
                )
                invoiceDao.insertInvoiceItems(
                    listOf(
                        InvoiceItemEntity(
                            invoiceId = inv2Id,
                            description = "Payment Gateway Integration (M-Pesa & Card)",
                            quantity = 1.0,
                            unitPrice = 750.0,
                            amount = 750.0
                        ),
                        InvoiceItemEntity(
                            invoiceId = inv2Id,
                            description = "Automated Order Notifications Setup",
                            quantity = 1.0,
                            unitPrice = 450.0,
                            amount = 450.0
                        )
                    )
                )

                // 3. Paid invoice
                val inv3Id = invoiceDao.insertInvoice(
                    InvoiceEntity(
                        invoiceNumber = "INV-1003",
                        clientId = clientId3,
                        issueDate = now - (25 * dayMs),
                        dueDate = now - (10 * dayMs),
                        subtotal = 600.0,
                        taxPercent = 0.0,
                        discountPercent = 50.0,
                        totalAmount = 550.00,
                        status = "PAID",
                        paidDate = now - (11 * dayMs),
                        paymentMethod = "Bank Transfer",
                        paymentReference = "TRX-829104",
                        notes = "Brand identity guidelines and social media templates."
                    )
                )
                invoiceDao.insertInvoiceItems(
                    listOf(
                        InvoiceItemEntity(
                            invoiceId = inv3Id,
                            description = "Brand Style Guide & Logo Polish",
                            quantity = 1.0,
                            unitPrice = 600.0,
                            amount = 600.0
                        )
                    )
                )
            }
        }
    }
}
