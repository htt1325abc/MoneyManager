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
import com.example.moneymanager.presentation.common.labelRes
import com.example.moneymanager.presentation.common.toMoneyFormat
import com.example.moneymanager.presentation.main.MoneyManagerUiState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MoneyManagerUiState) = with(binding) {
        balanceAmount.text = state.balance.toMoneyFormat()
        incomeAmount.text = getString(R.string.labeled_amount, getString(R.string.income), state.income.toMoneyFormat())
        expenseAmount.text = getString(R.string.labeled_amount, getString(R.string.expense), state.expense.toMoneyFormat())

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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
