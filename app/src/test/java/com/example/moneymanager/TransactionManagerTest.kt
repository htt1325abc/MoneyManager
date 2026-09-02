package com.example.moneymanager

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.service.TransactionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionManagerTest {
    private lateinit var manager: TransactionManager

    @Before
    fun setUp() {
        manager = TransactionManager()
        manager.addTransaction(transaction(1, "Lương", 12_000_000, TransactionType.INCOME, Category.SALARY, 100))
        manager.addTransaction(transaction(2, "Ăn sáng", 30_000, TransactionType.EXPENSE, Category.FOOD, 300))
        manager.addTransaction(transaction(3, "Xe buýt", 20_000, TransactionType.EXPENSE, Category.TRANSPORT, 200))
    }

    @Test
    fun addTransaction_addsAValidatedCopy() {
        manager.addTransaction(transaction(4, "  Mua sách  ", 150_000, TransactionType.EXPENSE, Category.EDUCATION, 400))

        assertEquals(4, manager.getTransactions().size)
        assertEquals("Mua sách", manager.getTransactions().last().title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTransaction_rejectsInvalidAmount() {
        manager.addTransaction(transaction(4, "Sai", 0, TransactionType.EXPENSE, Category.OTHER, 400))
    }

    @Test
    fun calculateTotals_returnsIncomeExpenseAndBalance() {
        assertEquals(12_000_000L, manager.calculateTotalIncome())
        assertEquals(50_000L, manager.calculateTotalExpense())
        assertEquals(11_950_000L, manager.calculateBalance())
    }

    @Test
    fun updateTransaction_keepsIdAndChangesContent() {
        val updated = transaction(2, "Bữa sáng", 35_000, TransactionType.EXPENSE, Category.FOOD, 300)

        assertTrue(manager.updateTransaction(updated))
        assertEquals(updated, manager.getTransactions().first { it.id == 2L })
        assertFalse(manager.updateTransaction(updated.copy(id = 99)))
    }

    @Test
    fun deleteTransaction_removesOnlyMatchingIdAndUpdatesTotals() {
        assertTrue(manager.deleteTransaction(2))
        assertFalse(manager.deleteTransaction(99))
        assertEquals(listOf(1L, 3L), manager.getTransactions().map { it.id })
        assertEquals(20_000L, manager.calculateTotalExpense())
    }

    @Test
    fun searchFilterAndSort_combinesAllCriteria() {
        manager.addTransaction(
            transaction(4, "Ăn tối", 80_000, TransactionType.EXPENSE, Category.FOOD, 400, "cùng bạn")
        )

        val result = manager.searchFilterAndSort(
            query = "ăn",
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            sort = TransactionSort.HIGHEST_AMOUNT,
        )

        assertEquals(listOf(80_000L, 30_000L), result.map { it.amount })
    }

    @Test
    fun expenseByCategory_groupsAndSumsExpensesOnly() {
        assertEquals(
            mapOf(Category.FOOD to 30_000L, Category.TRANSPORT to 20_000L),
            manager.expenseByCategory(),
        )
    }

    private fun transaction(
        id: Long,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        createdAt: Long,
        note: String = "",
    ) = Transaction(id, title, amount, type, category, note, createdAt)
}
