package com.example.data.repository

import com.example.data.local.dao.ClientDao
import com.example.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

class ClientRepository(private val clientDao: ClientDao) {

    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()

    suspend fun getClientById(id: Long): ClientEntity? = clientDao.getClientById(id)

    suspend fun insertClient(client: ClientEntity): Long = clientDao.insertClient(client)

    suspend fun updateClient(client: ClientEntity) = clientDao.updateClient(client)

    suspend fun deleteClient(client: ClientEntity) = clientDao.deleteClient(client)

    suspend fun getClientCount(): Int = clientDao.getClientCount()
}
