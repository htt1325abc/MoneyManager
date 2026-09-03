package com.example.moneymanager.service

import com.example.moneymanager.model.StatisticsPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class StatisticsDateRange(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate,
)

object StatisticsPeriodCalculator {
    fun rangeContaining(
        anchorDate: LocalDate,
        period: StatisticsPeriod,
    ): StatisticsDateRange {
        val start = when (period) {
            StatisticsPeriod.WEEK -> anchorDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            StatisticsPeriod.MONTH -> anchorDate.withDayOfMonth(1)
            StatisticsPeriod.YEAR -> anchorDate.withDayOfYear(1)
        }
        val endExclusive = when (period) {
            StatisticsPeriod.WEEK -> start.plusWeeks(1)
            StatisticsPeriod.MONTH -> start.plusMonths(1)
            StatisticsPeriod.YEAR -> start.plusYears(1)
        }
        return StatisticsDateRange(start, endExclusive)
    }

    fun move(
        anchorDate: LocalDate,
        period: StatisticsPeriod,
        amount: Long,
    ): LocalDate = when (period) {
        StatisticsPeriod.WEEK -> anchorDate.plusWeeks(amount)
        StatisticsPeriod.MONTH -> anchorDate.plusMonths(amount)
        StatisticsPeriod.YEAR -> anchorDate.plusYears(amount)
    }
}

