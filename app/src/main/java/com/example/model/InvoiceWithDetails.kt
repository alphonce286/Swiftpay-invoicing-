package com.example.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity

data class InvoiceWithDetails(
    @Embedded
    val invoice: InvoiceEntity,

    @Relation(
        parentColumn = "clientId",
        entityColumn = "id"
    )
    val client: ClientEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity> = emptyList()
)
