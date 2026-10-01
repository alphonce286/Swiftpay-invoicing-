package com.example.ui.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ClientEntity
import com.example.data.repository.ClientRepository
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.PaymentSettingsRepository
import com.example.model.InvoiceWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientListUiState(
    val clients: List<ClientEntity> = emptyList(),
    val filteredClients: List<ClientEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val clientRepo = ClientRepository(db.clientDao())
    private val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.reminderLogDao())
    private val settingsRepo = PaymentSettingsRepository(db.paymentSettingsDao())

    val settings = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<ClientListUiState> = combine(
        clientRepo.allClients,
        _searchQuery
    ) { clients, query ->
        val cleanQuery = query.trim()
        val normalizedQueryDigits = cleanQuery.filter { it.isDigit() }

        val filtered = clients.filter { client ->
            if (cleanQuery.isBlank()) return@filter true

            val matchesName = client.name.contains(cleanQuery, ignoreCase = true)
            val matchesCompany = client.company.contains(cleanQuery, ignoreCase = true)
            val matchesEmail = client.email.contains(cleanQuery, ignoreCase = true)
            val matchesPhone = client.phone.contains(cleanQuery, ignoreCase = true) ||
                    (normalizedQueryDigits.length >= 3 && client.phone.filter { it.isDigit() }.contains(normalizedQueryDigits))

            matchesName || matchesCompany || matchesEmail || matchesPhone
        }
        ClientListUiState(
            clients = clients,
            filteredClients = filtered,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientListUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveClient(
        id: Long = 0,
        name: String,
        phone: String,
        email: String,
        company: String,
        address: String,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val client = ClientEntity(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                company = company.trim(),
                address = address.trim(),
                notes = notes.trim()
            )
            if (id == 0L) {
                val newId = clientRepo.insertClient(client)
                onSuccess(newId)
            } else {
                clientRepo.updateClient(client)
                onSuccess(id)
            }
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            clientRepo.deleteClient(client)
        }
    }

    fun getClientInvoices(clientId: Long): StateFlow<List<InvoiceWithDetails>> {
        return invoiceRepo.getInvoicesForClient(clientId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}
