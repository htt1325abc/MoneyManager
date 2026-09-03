package com.example.moneymanager.service

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import java.time.ZoneId

data class TransactionSummary(
    val income: Long,
    val expense: Long,
) {
    val balance: Long = income - expense
}

object TransactionAnalytics {
    fun summary(transactions: List<Transaction>): TransactionSummary {
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        return TransactionSummary(income = income, expense = expense)
    }

    fun filterAndSort(
        transactions: List<Transaction>,
        query: String,
        type: TransactionType?,
        category: Category?,
        sort: TransactionSort,
    ): List<Transaction> {
        val normalizedQuery = query.trim()
        val filtered = transactions.asSequence()
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

    fun expenseByCategory(transactions: List<Transaction>): Map<Category, Long> =
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    fun transactionsInRange(
        transactions: List<Transaction>,
        range: StatisticsDateRange,
        zoneId: ZoneId,
    ): List<Transaction> {
        val startMillis = range.startInclusive
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endExclusiveMillis = range.endExclusive
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        return transactions.filter { transaction ->
            transaction.createdAt >= startMillis &&
                transaction.createdAt < endExclusiveMillis
        }
    }
}
