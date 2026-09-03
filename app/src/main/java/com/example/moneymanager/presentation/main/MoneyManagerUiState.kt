package com.example.moneymanager.presentation.main

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.StatisticsPeriod
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import java.time.LocalDate

sealed interface LoadState {
    data object Loading : LoadState
    data object Success : LoadState
    data class Error(val message: String) : LoadState
}

data class TransactionFilter(
    val type: TransactionType? = null,
    val category: Category? = null,
)

data class CategoryStatistic(
    val category: Category,
    val amount: Long,
    val percent: Int,
)

data class MonthlyStatistic(
    val label: String,
    val income: Long,
    val expense: Long,
    val sortKey: Int,
)

data class StatisticsSelection(
    val period: StatisticsPeriod = StatisticsPeriod.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
)

data class MoneyManagerUiState(
    val loadState: LoadState = LoadState.Loading,
    val balance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val allTransactions: List<Transaction> = emptyList(),
    val visibleTransactions: List<Transaction> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val monthlyStatistics: List<MonthlyStatistic> = emptyList(),
    val statisticsSelection: StatisticsSelection = StatisticsSelection(),
    val statisticsIncome: Long = 0,
    val statisticsExpense: Long = 0,
    val statisticsBalance: Long = 0,
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter(),
    val sort: TransactionSort = TransactionSort.NEWEST,
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
}
