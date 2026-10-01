package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PaymentSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentSettingsDao {
    @Query("SELECT * FROM payment_settings WHERE id = 1 LIMIT 1")
    fun getPaymentSettings(): Flow<PaymentSettingsEntity?>

    @Query("SELECT * FROM payment_settings WHERE id = 1 LIMIT 1")
    suspend fun getPaymentSettingsSync(): PaymentSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PaymentSettingsEntity)

    @Update
    suspend fun update(settings: PaymentSettingsEntity)
}
