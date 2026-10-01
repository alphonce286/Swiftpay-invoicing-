package com.example.data.repository

import com.example.data.local.dao.PaymentSettingsDao
import com.example.data.local.entity.PaymentSettingsEntity
import kotlinx.coroutines.flow.Flow

class PaymentSettingsRepository(private val settingsDao: PaymentSettingsDao) {

    val settings: Flow<PaymentSettingsEntity?> = settingsDao.getPaymentSettings()

    suspend fun getSettingsSync(): PaymentSettingsEntity {
        return settingsDao.getPaymentSettingsSync() ?: PaymentSettingsEntity()
    }

    suspend fun saveSettings(settings: PaymentSettingsEntity) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun toggleProTier(isPro: Boolean) {
        val current = getSettingsSync()
        settingsDao.insertOrUpdate(current.copy(isProTier = isPro))
    }
}
