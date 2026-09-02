package com.example.moneymanager.presentation.home

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymanager.MainActivity
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.databinding.FragmentHomeBinding
import com.example.moneymanager.presentation.common.TransactionAdapter
import com.example.moneymanager.presentation.common.confirmTransactionDeletion
import com.example.moneymanager.presentation.common.toMoneyFormat
import com.example.moneymanager.presentation.main.LoadState
import com.example.moneymanager.presentation.main.MoneyManagerUiState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: MoneyManagerViewModel by activityViewModels {
        val app = requireActivity().application as MoneyManagerApplication
        MoneyManagerViewModelFactory(app.transactionRepository)
    }

    private val adapter = TransactionAdapter(
        onEdit = { transaction -> (activity as? MainActivity)?.showTransactionForm(transaction.id) },
        onDelete = { transaction ->
            confirmTransactionDeletion(transaction) { viewModel.deleteTransaction(transaction) }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentHomeBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recentRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecycler.adapter = adapter
        binding.monthLabel.text = currentMonthLabel()
        binding.addButton.setOnClickListener { (activity as? MainActivity)?.showTransactionForm() }
        binding.viewAllButton.setOnClickListener { (activity as? MainActivity)?.showTransactions() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MoneyManagerUiState) = with(binding) {
        loadingProgress.isVisible = state.loadState is LoadState.Loading
        errorText.isVisible = state.loadState is LoadState.Error
        errorText.text = (state.loadState as? LoadState.Error)?.message.orEmpty()
        balanceAmount.text = state.balance.toMoneyFormat()
        incomeAmount.text = state.income.toMoneyFormat()
        expenseAmount.text = state.expense.toMoneyFormat()
        recentEmpty.isVisible = state.recentTransactions.isEmpty() && state.loadState is LoadState.Success
        recentRecycler.isVisible = state.recentTransactions.isNotEmpty()
        adapter.submitList(state.recentTransactions)
    }

    private fun currentMonthLabel(): String {
        val month = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("vi-VN"))
            .format(Date())
        return month.replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(Locale.forLanguageTag("vi-VN")) else character.toString()
        }
    }

    override fun onDestroyView() {
        binding.recentRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
