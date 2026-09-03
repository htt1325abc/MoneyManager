package com.example.moneymanager

import com.example.moneymanager.model.Category
import com.example.moneymanager.model.StatisticsPeriod
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.service.StatisticsDateRange
import com.example.moneymanager.service.StatisticsPeriodCalculator
import com.example.moneymanager.service.TransactionAnalytics
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsPeriodCalculatorTest {
    @Test
    fun week_startsOnMondayAndEndsBeforeNextMonday() {
        val range = StatisticsPeriodCalculator.rangeContaining(
            anchorDate = LocalDate.of(2026, 9, 3),
            period = StatisticsPeriod.WEEK,
        )

        assertEquals(LocalDate.of(2026, 8, 31), range.startInclusive)
        assertEquals(LocalDate.of(2026, 9, 7), range.endExclusive)
    }

    @Test
    fun monthAndYear_haveCorrectCalendarBoundaries() {
        val month = StatisticsPeriodCalculator.rangeContaining(
            LocalDate.of(2024, 2, 29),
            StatisticsPeriod.MONTH,
        )
        val year = StatisticsPeriodCalculator.rangeContaining(
            LocalDate.of(2024, 2, 29),
            StatisticsPeriod.YEAR,
        )

        assertEquals(LocalDate.of(2024, 2, 1), month.startInclusive)
        assertEquals(LocalDate.of(2024, 3, 1), month.endExclusive)
        assertEquals(LocalDate.of(2024, 1, 1), year.startInclusive)
        assertEquals(LocalDate.of(2025, 1, 1), year.endExclusive)
    }

    @Test
    fun transactionsInRange_includesStartAndExcludesEnd() {
        val range = StatisticsDateRange(
            startInclusive = LocalDate.of(2026, 9, 1),
            endExclusive = LocalDate.of(2026, 10, 1),
        )
        val start = range.startInclusive.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = range.endExclusive.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val transactions = listOf(
            transaction(1, start - 1),
            transaction(2, start),
            transaction(3, end - 1),
            transaction(4, end),
        )

        val result = TransactionAnalytics.transactionsInRange(
            transactions,
            range,
            ZoneOffset.UTC,
        )

        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    private fun transaction(id: Long, createdAt: Long) = Transaction(
        id = id,
        title = "Giao dịch $id",
        amount = 10_000,
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        createdAt = createdAt,
    )
}

