package com.example.moneymanager

import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.presentation.main.LoadState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyManagerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun roomLikeFlow_updatesStateAndDerivedValues() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = MoneyManagerViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.emit(
            listOf(
                transaction(1, "Lương", 5_000_000, TransactionType.INCOME, Category.SALARY, 100),
                transaction(2, "Ăn trưa", 50_000, TransactionType.EXPENSE, Category.FOOD, 200),
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.loadState is LoadState.Success)
        assertEquals(4_950_000L, state.balance)
        assertEquals(listOf(2L, 1L), state.recentTransactions.map { it.id })
    }

    @Test
    fun queryFilterAndSort_updateVisibleTransactions() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = MoneyManagerViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        repository.emit(
            listOf(
                transaction(1, "Ăn sáng", 30_000, TransactionType.EXPENSE, Category.FOOD, 100),
                transaction(2, "Ăn tối", 80_000, TransactionType.EXPENSE, Category.FOOD, 200),
                transaction(3, "Lương", 5_000_000, TransactionType.INCOME, Category.SALARY, 300),
            )
        )

        viewModel.updateQuery("ăn")
        viewModel.updateTypeFilter(TransactionType.EXPENSE)
        viewModel.updateCategoryFilter(Category.FOOD)
        viewModel.updateSort(TransactionSort.LOWEST_AMOUNT)
        advanceUntilIdle()

        assertEquals(listOf(30_000L, 80_000L), viewModel.uiState.value.visibleTransactions.map { it.amount })
    }

    @Test
    fun writeActions_delegateToRepository() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = MoneyManagerViewModel(repository)
        val item = transaction(1, "Ăn trưa", 50_000, TransactionType.EXPENSE, Category.FOOD, 100)

        viewModel.addTransaction(item.copy(id = 0))
        viewModel.updateTransaction(item.copy(title = "Bữa trưa"))
        viewModel.deleteTransaction(item)
        advanceUntilIdle()

        assertEquals(1, repository.added.size)
        assertEquals("Bữa trưa", repository.updated.single().title)
        assertEquals(item, repository.deleted.single())
    }

    private fun transaction(
        id: Long,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        createdAt: Long,
    ) = Transaction(id, title, amount, type, category, createdAt = createdAt)

    private class FakeTransactionRepository : TransactionRepository {
        private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
        val added = mutableListOf<Transaction>()
        val updated = mutableListOf<Transaction>()
        val deleted = mutableListOf<Transaction>()

        override fun getTransactions(): Flow<List<Transaction>> = transactions

        override suspend fun addTransaction(transaction: Transaction) {
            added += transaction
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            updated += transaction
        }

        override suspend fun deleteTransaction(transaction: Transaction) {
            deleted += transaction
        }

        fun emit(items: List<Transaction>) {
            transactions.value = items
        }
    }
}
