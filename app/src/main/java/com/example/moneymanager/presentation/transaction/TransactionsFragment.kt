package com.example.moneymanager.presentation.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymanager.MainActivity
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.R
import com.example.moneymanager.databinding.FragmentTransactionsBinding
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.presentation.common.TransactionAdapter
import com.example.moneymanager.presentation.common.confirmTransactionDeletion
import com.example.moneymanager.presentation.common.labelRes
import com.example.moneymanager.presentation.main.LoadState
import com.example.moneymanager.presentation.main.MoneyManagerUiState
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
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
    ): View = FragmentTransactionsBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transactionRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecycler.adapter = adapter
        binding.searchEditText.doAfterTextChanged { text -> viewModel.updateQuery(text?.toString().orEmpty()) }
        binding.typeFilterButton.setOnClickListener(::showTypeMenu)
        binding.categoryFilterButton.setOnClickListener(::showCategoryMenu)
        binding.sortButton.setOnClickListener(::showSortMenu)
        binding.addButton.setOnClickListener { (activity as? MainActivity)?.showTransactionForm() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MoneyManagerUiState) = with(binding) {
        val isLoading = state.loadState is LoadState.Loading
        val errorMessage = (state.loadState as? LoadState.Error)?.message
        loadingProgress.isVisible = isLoading
        adapter.submitList(state.visibleTransactions)
        transactionRecycler.isVisible = !isLoading && state.visibleTransactions.isNotEmpty()
        emptyText.isVisible = !isLoading && state.visibleTransactions.isEmpty()
        emptyText.text = errorMessage ?: getString(
            if (state.allTransactions.isEmpty()) R.string.empty_transactions_body else R.string.empty_search,
        )
        typeFilterButton.text = when (state.filter.type) {
            null -> getString(R.string.filter_all)
            TransactionType.INCOME -> getString(R.string.filter_income)
            TransactionType.EXPENSE -> getString(R.string.filter_expense)
        }
        categoryFilterButton.text = state.filter.category
            ?.let { getString(R.string.filter_category, getString(it.labelRes)) }
            ?: getString(R.string.category_hint)
        sortButton.text = getString(R.string.sort_label, getString(state.sort.labelRes))
    }

    private fun showTypeMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(Menu.NONE, TYPE_ALL, Menu.NONE, R.string.filter_all)
            menu.add(Menu.NONE, TYPE_INCOME, Menu.NONE, R.string.filter_income)
            menu.add(Menu.NONE, TYPE_EXPENSE, Menu.NONE, R.string.filter_expense)
            setOnMenuItemClickListener { item ->
                viewModel.updateTypeFilter(
                    when (item.itemId) {
                        TYPE_INCOME -> TransactionType.INCOME
                        TYPE_EXPENSE -> TransactionType.EXPENSE
                        else -> null
                    }
                )
                true
            }
        }.show()
    }

    private fun showCategoryMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(Menu.NONE, CATEGORY_ALL, Menu.NONE, R.string.filter_all)
            Category.entries.forEachIndexed { index, category ->
                menu.add(Menu.NONE, CATEGORY_OFFSET + index, Menu.NONE, category.labelRes)
            }
            setOnMenuItemClickListener { item ->
                val category = Category.entries.getOrNull(item.itemId - CATEGORY_OFFSET)
                viewModel.updateCategoryFilter(category)
                true
            }
        }.show()
    }

    private fun showSortMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            TransactionSort.entries.forEachIndexed { index, sort ->
                menu.add(Menu.NONE, SORT_OFFSET + index, Menu.NONE, sort.labelRes)
            }
            setOnMenuItemClickListener { item ->
                TransactionSort.entries.getOrNull(item.itemId - SORT_OFFSET)
                    ?.let(viewModel::updateSort)
                true
            }
        }.show()
    }

    override fun onDestroyView() {
        binding.transactionRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TYPE_ALL = 1
        const val TYPE_INCOME = 2
        const val TYPE_EXPENSE = 3
        const val CATEGORY_ALL = 100
        const val CATEGORY_OFFSET = 101
        const val SORT_OFFSET = 201
    }
}
