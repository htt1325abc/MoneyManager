package com.example.moneymanager.data.repository

import com.example.moneymanager.data.local.TransactionDao
import com.example.moneymanager.data.mapper.toDomain
import com.example.moneymanager.data.mapper.toEntity
import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
) : TransactionRepository {
    override fun getTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.copy(id = 0).toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }
}
