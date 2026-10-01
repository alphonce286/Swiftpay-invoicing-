package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.model.InvoiceWithDetails
import com.example.util.CurrencyFormatter
import com.example.util.ReminderTiming
import com.example.util.ReminderTone
import com.example.util.ReminderTemplateHelper
import com.example.util.SharingHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("InvoiceFlow", appName)
    }

    @Test
    fun `currency formatter formats standard amounts`() {
        val formatted = CurrencyFormatter.format(1250.50, "$")
        assertEquals("$1,250.50", formatted)
    }

    @Test
    fun `reminder template helper builds whatsapp formatted message`() {
        val invoice = InvoiceEntity(
            id = 1,
            invoiceNumber = "INV-1001",
            clientId = 10,
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            subtotal = 500.0,
            totalAmount = 500.0,
            status = "PENDING"
        )
        val client = ClientEntity(
            id = 10,
            name = "Jane Doe",
            phone = "+1555123456",
            email = "jane@example.com"
        )
        val details = InvoiceWithDetails(invoice = invoice, client = client, items = emptyList())
        val settings = PaymentSettingsEntity(businessName = "Alpha Studio")

        val message = ReminderTemplateHelper.generateMessage(
            invoiceWithDetails = details,
            settings = settings,
            timing = ReminderTiming.BEFORE_DUE,
            tone = ReminderTone.FRIENDLY
        )

        assertTrue(message.contains("Jane Doe"))
        assertTrue(message.contains("INV-1001"))
        assertTrue(message.contains("Alpha Studio"))
    }

    @Test
    fun `client schema supports name phone email and company`() {
        val client = ClientEntity(
            id = 1,
            name = "Marcus Aurelius",
            phone = "+1 (555) 987-6543",
            email = "marcus@rome.org",
            company = "Stoic Enterprises",
            address = "Via Sacra 1, Rome",
            notes = "Net 30 terms"
        )
        assertEquals("Marcus Aurelius", client.name)
        assertEquals("+1 (555) 987-6543", client.phone)
        assertEquals("marcus@rome.org", client.email)
        assertEquals("Stoic Enterprises", client.company)
    }

    @Test
    fun `generate deep link and build share bundle includes invoice id and url`() {
        val deepLink = SharingHelper.generateDeepLink(42L, "INV-1042")
        assertEquals("https://invoiceflow.app/invoice/42", deepLink)

        val invoice = InvoiceEntity(
            id = 42,
            invoiceNumber = "INV-1042",
            clientId = 5,
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            subtotal = 300.0,
            totalAmount = 300.0,
            status = "PENDING"
        )
        val client = ClientEntity(id = 5, name = "Alex Smith", phone = "+123456789", email = "alex@test.com")
        val bundle = SharingHelper.buildShareBundle(
            invoiceWithDetails = InvoiceWithDetails(invoice, client, emptyList()),
            settings = PaymentSettingsEntity(businessName = "Freelance Pro", onlinePaymentUrl = "https://pay.stripe.com/test")
        )

        assertEquals("https://invoiceflow.app/invoice/42", bundle.deepLink)
        assertTrue(bundle.whatsAppMessage.contains("https://invoiceflow.app/invoice/42"))
        assertTrue(bundle.smsBody.contains("https://invoiceflow.app/invoice/42"))
        assertTrue(bundle.emailBody.contains("https://invoiceflow.app/invoice/42"))
        assertTrue(bundle.subject.contains("INV-1042"))
    }

    @Test
    fun `room database persists client and invoice data locally`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val clientRepo = com.example.data.repository.ClientRepository(db.clientDao())
        val invoiceRepo = com.example.data.repository.InvoiceRepository(db.invoiceDao(), db.reminderLogDao())

        // 1. Insert Client locally
        val clientId = clientRepo.insertClient(
            ClientEntity(
                name = "Test Client Corp",
                phone = "+15554443322",
                email = "billing@testclient.com",
                company = "Test Client LLC"
            )
        )
        assertTrue(clientId > 0)

        val retrievedClient = clientRepo.getClientById(clientId)
        assertEquals("Test Client Corp", retrievedClient?.name)
        assertEquals("+15554443322", retrievedClient?.phone)

        // 2. Create and persist Invoice with Items locally
        val invoice = InvoiceEntity(
            invoiceNumber = "INV-TEST-01",
            clientId = clientId,
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            subtotal = 1000.0,
            taxPercent = 10.0,
            discountPercent = 5.0,
            totalAmount = 1045.0,
            status = "PENDING"
        )
        val items = listOf(
            InvoiceItemEntity(
                invoiceId = 0,
                description = "Design Service",
                quantity = 2.0,
                unitPrice = 500.0,
                amount = 1000.0
            )
        )
        val invoiceId = invoiceRepo.createInvoice(invoice, items)
        assertTrue(invoiceId > 0)

        // 3. Mark invoice as paid and verify state update
        invoiceRepo.markPaid(invoiceId, "Bank Transfer")

        val updatedInvoice = db.invoiceDao().getInvoiceById(invoiceId)
        assertEquals("PAID", updatedInvoice?.status)
        assertEquals("Bank Transfer", updatedInvoice?.paymentMethod)

        db.close()
    }

    @Test
    fun `search filter matches clients by name phone and email`() {
        val clients = listOf(
            ClientEntity(id = 1, name = "Alice Johnson", phone = "+1 (555) 234-5678", email = "alice@example.com"),
            ClientEntity(id = 2, name = "Bob Martinez", phone = "+254 712 345678", email = "bob@techcorp.io"),
            ClientEntity(id = 3, name = "Charlie Brown", phone = "+44 20 7946 0192", email = "charlie@peanuts.org")
        )

        fun filterClients(query: String): List<ClientEntity> {
            val cleanQuery = query.trim()
            val digits = cleanQuery.filter { it.isDigit() }
            return clients.filter { client ->
                if (cleanQuery.isBlank()) return@filter true
                client.name.contains(cleanQuery, ignoreCase = true) ||
                        client.email.contains(cleanQuery, ignoreCase = true) ||
                        client.phone.contains(cleanQuery, ignoreCase = true) ||
                        (digits.length >= 3 && client.phone.filter { it.isDigit() }.contains(digits))
            }
        }

        // Search by name
        val nameMatch = filterClients("Alice")
        assertEquals(1, nameMatch.size)
        assertEquals("Alice Johnson", nameMatch.first().name)

        // Search by email
        val emailMatch = filterClients("peanuts.org")
        assertEquals(1, emailMatch.size)
        assertEquals("Charlie Brown", emailMatch.first().name)

        // Search by phone digits
        val phoneMatch = filterClients("712345")
        assertEquals(1, phoneMatch.size)
        assertEquals("Bob Martinez", phoneMatch.first().name)
    }

    @Test
    fun `form validation checks required client, item description, positive amount, and due date`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.invoice.InvoiceViewModel(app)

        // Initial blank state -> validation fails
        val initialValidation = viewModel.validateForm()
        org.junit.Assert.assertFalse(initialValidation.isValid)
        org.junit.Assert.assertNotNull(initialValidation.clientError)
        org.junit.Assert.assertNotNull(initialValidation.amountError)

        // 1. Select a client
        val client = ClientEntity(id = 1, name = "Test Client", phone = "123456", email = "test@client.com")
        viewModel.setCreateClient(client)

        // 2. Set description and positive price/qty
        viewModel.updateLineItem(0, "Web Development", 1.0, 500.0)

        // 3. Set valid due date (future)
        val futureDueDate = System.currentTimeMillis() + 86400000L * 7
        viewModel.setCustomDueDate(futureDueDate)

        // Now validation must pass
        val validResult = viewModel.validateForm()
        org.junit.Assert.assertTrue("Form should be valid now: ${validResult.generalErrorMessage}", validResult.isValid)
        org.junit.Assert.assertNull(validResult.clientError)
        org.junit.Assert.assertNull(validResult.dueDateError)
        org.junit.Assert.assertNull(validResult.amountError)
        org.junit.Assert.assertTrue(validResult.itemDescriptionErrors.isEmpty())
        org.junit.Assert.assertTrue(validResult.itemPriceErrors.isEmpty())

        // If user clears description -> validation fails
        viewModel.updateLineItem(0, "", 1.0, 500.0)
        val blankDescResult = viewModel.validateForm()
        org.junit.Assert.assertFalse(blankDescResult.isValid)
        org.junit.Assert.assertTrue(blankDescResult.itemDescriptionErrors.contains(0))

        // If user sets rate to 0 -> validation fails
        viewModel.updateLineItem(0, "Web Development", 1.0, 0.0)
        val zeroPriceResult = viewModel.validateForm()
        org.junit.Assert.assertFalse(zeroPriceResult.isValid)
        org.junit.Assert.assertTrue(zeroPriceResult.itemPriceErrors.contains(0))
    }
}
