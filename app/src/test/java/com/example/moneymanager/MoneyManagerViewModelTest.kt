package com.example.moneymanager

import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.StatisticsPeriod
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.presentation.main.LoadState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
    fun statisticsPeriod_filtersAndMovesToPreviousPeriod() = runTest {
        val repository = FakeTransactionRepository()
        val clock = Clock.fixed(
            Instant.parse("2026-09-03T12:00:00Z"),
            ZoneOffset.UTC,
        )
        val viewModel = MoneyManagerViewModel(repository, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        repository.emit(
            listOf(
                transaction(
                    1,
                    "Lương tháng 9",
                    10_000_000,
                    TransactionType.INCOME,
                    Category.SALARY,
                    dateMillis(2026, 9, 1),
                ),
                transaction(
                    2,
                    "Ăn trưa tháng 9",
                    100_000,
                    TransactionType.EXPENSE,
                    Category.FOOD,
                    dateMillis(2026, 9, 2),
                ),
                transaction(
                    3,
                    "Mua sắm tuần trước",
                    900_000,
                    TransactionType.EXPENSE,
                    Category.SHOPPING,
                    dateMillis(2026, 8, 30),
                ),
            )
        )
        advanceUntilIdle()

        assertEquals(10_000_000L, viewModel.uiState.value.statisticsIncome)
        assertEquals(100_000L, viewModel.uiState.value.statisticsExpense)

        viewModel.updateStatisticsPeriod(StatisticsPeriod.WEEK)
        viewModel.showPreviousStatisticsPeriod()
        advanceUntilIdle()

        val previousWeek = viewModel.uiState.value
        assertEquals(0L, previousWeek.statisticsIncome)
        assertEquals(900_000L, previousWeek.statisticsExpense)
        assertEquals(Category.SHOPPING, previousWeek.categoryStatistics.single().category)
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

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

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
