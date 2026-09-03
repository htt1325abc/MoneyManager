package com.example.moneymanager.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.StatisticsPeriod
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.service.TransactionAnalytics
import com.example.moneymanager.service.StatisticsPeriodCalculator
import java.time.Clock
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoneyManagerViewModel(
    private val repository: TransactionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TransactionFilter())
    private val sort = MutableStateFlow(TransactionSort.NEWEST)
    private val statisticsSelection = MutableStateFlow(
        StatisticsSelection(anchorDate = LocalDate.now(clock))
    )

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<MoneyManagerUiState> = combine(
        repository.getTransactions(),
        query,
        filter,
        sort,
        statisticsSelection,
    ) { transactions, currentQuery, currentFilter, currentSort, currentStatisticsSelection ->
        createUiState(
            transactions,
            currentQuery,
            currentFilter,
            currentSort,
            currentStatisticsSelection,
        )
    }.catch {
        emit(
            MoneyManagerUiState(
                loadState = LoadState.Error("Không thể đọc dữ liệu đã lưu."),
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoneyManagerUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun updateTypeFilter(type: TransactionType?) {
        filter.value = filter.value.copy(type = type)
    }

    fun updateCategoryFilter(category: Category?) {
        filter.value = filter.value.copy(category = category)
    }

    fun updateSort(value: TransactionSort) {
        sort.value = value
    }

    fun updateStatisticsPeriod(period: StatisticsPeriod) {
        val current = statisticsSelection.value
        if (current.period == period) return
        statisticsSelection.value = current.copy(period = period)
    }

    fun showPreviousStatisticsPeriod() {
        moveStatisticsPeriod(-1)
    }

    fun showNextStatisticsPeriod() {
        moveStatisticsPeriod(1)
    }

    fun addTransaction(transaction: Transaction) {
        performWrite("Đã thêm giao dịch") {
            repository.addTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        performWrite("Đã cập nhật giao dịch") {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        performWrite("Đã xóa giao dịch") {
            repository.deleteTransaction(transaction)
        }
    }

    private fun performWrite(successMessage: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
                _events.emit(UiEvent.ShowMessage(successMessage))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _events.emit(UiEvent.ShowMessage("Không thể lưu thay đổi. Hãy thử lại."))
            }
        }
    }

    private fun createUiState(
        transactions: List<Transaction>,
        currentQuery: String,
        currentFilter: TransactionFilter,
        currentSort: TransactionSort,
        currentStatisticsSelection: StatisticsSelection,
    ): MoneyManagerUiState {
        val summary = TransactionAnalytics.summary(transactions)
        val visible = TransactionAnalytics.filterAndSort(
            transactions = transactions,
            query = currentQuery,
            type = currentFilter.type,
            category = currentFilter.category,
            sort = currentSort,
        )
        val statisticsRange = StatisticsPeriodCalculator.rangeContaining(
            anchorDate = currentStatisticsSelection.anchorDate,
            period = currentStatisticsSelection.period,
        )
        val statisticsTransactions = TransactionAnalytics.transactionsInRange(
            transactions = transactions,
            range = statisticsRange,
            zoneId = clock.zone,
        )
        val statisticsSummary = TransactionAnalytics.summary(statisticsTransactions)
        val categoryStatistics = TransactionAnalytics.expenseByCategory(statisticsTransactions)
            .map { (category, amount) ->
                val percent = if (statisticsSummary.expense == 0L) {
                    0
                } else {
                    (amount * 100 / statisticsSummary.expense).toInt()
                }
                CategoryStatistic(category, amount, percent)
            }
            .sortedByDescending { it.amount }

        return MoneyManagerUiState(
            loadState = LoadState.Success,
            balance = summary.balance,
            income = summary.income,
            expense = summary.expense,
            allTransactions = transactions,
            visibleTransactions = visible,
            recentTransactions = transactions.sortedByDescending { it.createdAt }.take(5),
            categoryStatistics = categoryStatistics,
            monthlyStatistics = monthlyStatistics(transactions),
            statisticsSelection = currentStatisticsSelection,
            statisticsIncome = statisticsSummary.income,
            statisticsExpense = statisticsSummary.expense,
            statisticsBalance = statisticsSummary.balance,
            query = currentQuery,
            filter = currentFilter,
            sort = currentSort,
        )
    }

    private fun moveStatisticsPeriod(amount: Long) {
        val current = statisticsSelection.value
        statisticsSelection.value = current.copy(
            anchorDate = StatisticsPeriodCalculator.move(
                anchorDate = current.anchorDate,
                period = current.period,
                amount = amount,
            )
        )
    }

    private fun monthlyStatistics(transactions: List<Transaction>): List<MonthlyStatistic> {
        val keyFormatter = SimpleDateFormat("yyyyMM", Locale.US)
        val labelFormatter = SimpleDateFormat("MM/yyyy", Locale.forLanguageTag("vi-VN"))

        return transactions
            .groupBy { keyFormatter.format(Date(it.createdAt)).toInt() }
            .map { (sortKey, items) ->
                MonthlyStatistic(
                    label = labelFormatter.format(Date(items.first().createdAt)),
                    income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    sortKey = sortKey,
                )
            }
            .sortedByDescending { it.sortKey }
    }
}
