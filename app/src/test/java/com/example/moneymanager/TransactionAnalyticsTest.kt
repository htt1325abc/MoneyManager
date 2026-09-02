package com.example.moneymanager

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.service.TransactionAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionAnalyticsTest {
    private val transactions = listOf(
        Transaction(1, "Lương", 10_000_000, TransactionType.INCOME, Category.SALARY, createdAt = 100),
        Transaction(2, "Ăn trưa", 50_000, TransactionType.EXPENSE, Category.FOOD, createdAt = 300),
        Transaction(3, "Ăn tối", 80_000, TransactionType.EXPENSE, Category.FOOD, createdAt = 200),
        Transaction(4, "Xe buýt", 20_000, TransactionType.EXPENSE, Category.TRANSPORT, createdAt = 400),
    )

    @Test
    fun summary_calculatesIncomeExpenseAndBalance() {
        val summary = TransactionAnalytics.summary(transactions)

        assertEquals(10_000_000L, summary.income)
        assertEquals(150_000L, summary.expense)
        assertEquals(9_850_000L, summary.balance)
    }

    @Test
    fun filterAndSort_appliesQueryTypeCategoryAndAmountOrder() {
        val result = TransactionAnalytics.filterAndSort(
            transactions = transactions,
            query = "ăn",
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            sort = TransactionSort.LOWEST_AMOUNT,
        )

        assertEquals(listOf(50_000L, 80_000L), result.map { it.amount })
    }

    @Test
    fun expenseByCategory_excludesIncomeAndGroupsExpenses() {
        assertEquals(
            mapOf(Category.FOOD to 130_000L, Category.TRANSPORT to 20_000L),
            TransactionAnalytics.expenseByCategory(transactions),
        )
    }
}
