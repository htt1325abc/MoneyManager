package com.example.moneymanager.service

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType

class TransactionManager {
    private val transactions = mutableListOf<Transaction>()

    fun addTransaction(transaction: Transaction) {
        require(transaction.title.isNotBlank()) { "Title must not be blank" }
        require(transaction.amount > 0) { "Amount must be greater than zero" }
        transactions.add(transaction.copy(title = transaction.title.trim()))
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        require(transaction.title.isNotBlank()) { "Title must not be blank" }
        require(transaction.amount > 0) { "Amount must be greater than zero" }

        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index == -1) return false

        transactions[index] = transaction.copy(title = transaction.title.trim())
        return true
    }

    fun deleteTransaction(id: Long): Boolean =
        transactions.removeAll { it.id == id }

    fun getTransactions(): List<Transaction> = transactions.toList()

    fun calculateTotalIncome(): Long = transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }

    fun calculateTotalExpense(): Long = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    fun calculateBalance(): Long = calculateTotalIncome() - calculateTotalExpense()

    fun searchFilterAndSort(
        query: String = "",
        type: TransactionType? = null,
        category: Category? = null,
        sort: TransactionSort = TransactionSort.NEWEST,
    ): List<Transaction> {
        val normalizedQuery = query.trim()
        val filtered = transactions
            .asSequence()
            .filter { type == null || it.type == type }
            .filter { category == null || it.category == category }
            .filter { transaction ->
                normalizedQuery.isEmpty() ||
                    transaction.title.contains(normalizedQuery, ignoreCase = true) ||
                    transaction.note.contains(normalizedQuery, ignoreCase = true)
            }

        return when (sort) {
            TransactionSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            TransactionSort.OLDEST -> filtered.sortedBy { it.createdAt }
            TransactionSort.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            TransactionSort.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }.toList()
    }

    fun expenseByCategory(): Map<Category, Long> = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
}
