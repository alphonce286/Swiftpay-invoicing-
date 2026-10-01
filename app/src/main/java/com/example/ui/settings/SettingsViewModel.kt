package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PaymentSettingsEntity
import com.example.data.repository.PaymentSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = PaymentSettingsRepository(db.paymentSettingsDao())

    val settings: StateFlow<PaymentSettingsEntity> = settingsRepo.settings
        .map { it ?: PaymentSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PaymentSettingsEntity()
        )

    private val _isSavedSuccess = MutableStateFlow(false)
    val isSavedSuccess = _isSavedSuccess.asStateFlow()

    fun saveSettings(updated: PaymentSettingsEntity) {
        viewModelScope.launch {
            settingsRepo.saveSettings(updated)
            _isSavedSuccess.value = true
        }
    }

    fun toggleProTier(isPro: Boolean) {
        viewModelScope.launch {
            settingsRepo.toggleProTier(isPro)
        }
    }

    fun resetSaveSuccess() {
        _isSavedSuccess.value = false
    }
}
