package com.example.moneymanager.presentation.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.R
import com.example.moneymanager.databinding.FragmentStatisticsBinding
import com.example.moneymanager.databinding.ItemStatisticBinding
import com.example.moneymanager.model.StatisticsPeriod
import com.example.moneymanager.presentation.common.labelRes
import com.example.moneymanager.presentation.common.toMoneyFormat
import com.example.moneymanager.presentation.main.MoneyManagerUiState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
import com.example.moneymanager.service.StatisticsPeriodCalculator
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: MoneyManagerViewModel by activityViewModels {
        val app = requireActivity().application as MoneyManagerApplication
        MoneyManagerViewModelFactory(app.transactionRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentStatisticsBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurePeriodControls()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MoneyManagerUiState) = with(binding) {
        val selectedButtonId = when (state.statisticsSelection.period) {
            StatisticsPeriod.WEEK -> R.id.weekButton
            StatisticsPeriod.MONTH -> R.id.monthButton
            StatisticsPeriod.YEAR -> R.id.yearButton
        }
        if (periodGroup.checkedButtonId != selectedButtonId) {
            periodGroup.check(selectedButtonId)
        }
        periodLabel.text = formatPeriodLabel(state)

        balanceAmount.text = state.statisticsBalance.toMoneyFormat()
        incomeAmount.text = getString(
            R.string.labeled_amount,
            getString(R.string.income),
            state.statisticsIncome.toMoneyFormat(),
        )
        expenseAmount.text = getString(
            R.string.labeled_amount,
            getString(R.string.expense),
            state.statisticsExpense.toMoneyFormat(),
        )

        categoryEmpty.isVisible = state.categoryStatistics.isEmpty()
        categoryContainer.removeAllViews()
        state.categoryStatistics.forEach { statistic ->
            val row = ItemStatisticBinding.inflate(layoutInflater, categoryContainer, false)
            row.labelText.text = getString(statistic.category.labelRes)
            row.valueText.text = getString(
                R.string.category_stat_amount,
                statistic.amount.toMoneyFormat(),
                statistic.percent,
            )
            row.progress.progress = statistic.percent
            categoryContainer.addView(row.root)
        }

        monthlyContainer.removeAllViews()
        state.monthlyStatistics.forEach { statistic ->
            val row = ItemStatisticBinding.inflate(layoutInflater, monthlyContainer, false)
            val total = statistic.income + statistic.expense
            row.labelText.text = statistic.label
            row.valueText.text = getString(
                R.string.monthly_stat_amount,
                statistic.income.toMoneyFormat(),
                statistic.expense.toMoneyFormat(),
            )
            row.progress.progress = if (total == 0L) 0 else (statistic.expense * 100 / total).toInt()
            monthlyContainer.addView(row.root)
        }
    }

    private fun configurePeriodControls() = with(binding) {
        periodGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val period = when (checkedId) {
                R.id.weekButton -> StatisticsPeriod.WEEK
                R.id.yearButton -> StatisticsPeriod.YEAR
                else -> StatisticsPeriod.MONTH
            }
            viewModel.updateStatisticsPeriod(period)
        }
        previousPeriodButton.setOnClickListener {
            viewModel.showPreviousStatisticsPeriod()
        }
        nextPeriodButton.setOnClickListener {
            viewModel.showNextStatisticsPeriod()
        }
    }

    private fun formatPeriodLabel(state: MoneyManagerUiState): String {
        val selection = state.statisticsSelection
        val range = StatisticsPeriodCalculator.rangeContaining(
            anchorDate = selection.anchorDate,
            period = selection.period,
        )
        val locale = Locale.forLanguageTag("vi-VN")

        return when (selection.period) {
            StatisticsPeriod.WEEK -> {
                val startFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
                val endFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
                val endInclusive = range.endExclusive.minusDays(1)
                "${range.startInclusive.format(startFormatter)} – ${endInclusive.format(endFormatter)}"
            }
            StatisticsPeriod.MONTH -> range.startInclusive
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                .replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase(locale) else character.toString()
                }
            StatisticsPeriod.YEAR -> getString(
                R.string.period_year_label,
                range.startInclusive.year,
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
