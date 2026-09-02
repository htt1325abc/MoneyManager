package com.example.moneymanager.presentation.transaction

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.R
import com.example.moneymanager.databinding.DialogTransactionFormBinding
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionType
import com.example.moneymanager.presentation.common.labelRes
import com.example.moneymanager.presentation.common.toDateLabel
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class TransactionFormDialogFragment : DialogFragment() {
    private val viewModel: MoneyManagerViewModel by activityViewModels {
        val app = requireActivity().application as MoneyManagerApplication
        MoneyManagerViewModelFactory(app.transactionRepository)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTransactionFormBinding.inflate(layoutInflater)
        val transactionId = arguments?.getLong(ARG_TRANSACTION_ID)?.takeIf { it > 0 }
        val currentTransaction = transactionId?.let { id ->
            viewModel.uiState.value.allTransactions.firstOrNull { transaction -> transaction.id == id }
        }

        var selectedType = currentTransaction?.type ?: TransactionType.EXPENSE
        var selectedCategory = currentTransaction?.category ?: Category.FOOD
        var selectedDate = currentTransaction?.createdAt ?: System.currentTimeMillis()

        configureTypeDropdown(binding, selectedType) { selectedType = it }
        configureCategoryDropdown(binding, selectedCategory) { selectedCategory = it }
        binding.dateEditText.setText(selectedDate.toDateLabel())
        binding.dateEditText.setOnClickListener {
            showDatePicker(selectedDate) { newDate ->
                selectedDate = newDate
                binding.dateEditText.setText(newDate.toDateLabel())
            }
        }

        currentTransaction?.let { transaction ->
            binding.titleEditText.setText(transaction.title)
            binding.amountEditText.setText(getString(R.string.amount_input, transaction.amount))
            binding.noteEditText.setText(transaction.note)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (currentTransaction == null) R.string.add_transaction else R.string.edit_transaction)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
                val amount = binding.amountEditText.text
                    ?.toString()
                    ?.trim()
                    ?.toLongOrNull()

                binding.titleInput.error = if (title.isBlank()) getString(R.string.title_required) else null
                binding.amountInput.error = if (amount == null || amount <= 0) {
                    getString(R.string.amount_invalid)
                } else {
                    null
                }

                if (title.isBlank() || amount == null || amount <= 0) return@setOnClickListener

                val transaction = currentTransaction?.copy(
                    title = title,
                    amount = amount,
                    type = selectedType,
                    category = selectedCategory,
                    note = binding.noteEditText.text?.toString()?.trim().orEmpty(),
                    createdAt = selectedDate,
                ) ?: Transaction(
                    title = title,
                    amount = amount,
                    type = selectedType,
                    category = selectedCategory,
                    note = binding.noteEditText.text?.toString()?.trim().orEmpty(),
                    createdAt = selectedDate,
                )

                if (currentTransaction == null) {
                    viewModel.addTransaction(transaction)
                } else {
                    viewModel.updateTransaction(transaction)
                }
                dismiss()
            }
        }
        return dialog
    }

    private fun configureTypeDropdown(
        binding: DialogTransactionFormBinding,
        initial: TransactionType,
        onSelected: (TransactionType) -> Unit,
    ) {
        val values = TransactionType.entries
        val labels = values.map { getString(it.labelRes) }
        binding.typeDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
        )
        binding.typeDropdown.setText(getString(initial.labelRes), false)
        binding.typeDropdown.setOnItemClickListener { _, _, position, _ -> onSelected(values[position]) }
    }

    private fun configureCategoryDropdown(
        binding: DialogTransactionFormBinding,
        initial: Category,
        onSelected: (Category) -> Unit,
    ) {
        val values = Category.entries
        val labels = values.map { getString(it.labelRes) }
        binding.categoryDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
        )
        binding.categoryDropdown.setText(getString(initial.labelRes), false)
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ -> onSelected(values[position]) }
    }

    private fun showDatePicker(initialDate: Long, onSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onSelected(selected.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    companion object {
        const val TAG = "transaction-form"
        private const val ARG_TRANSACTION_ID = "transaction-id"

        fun newInstance(transactionId: Long?): TransactionFormDialogFragment =
            TransactionFormDialogFragment().apply {
                arguments = Bundle().apply {
                    transactionId?.let { putLong(ARG_TRANSACTION_ID, it) }
                }
            }
    }
}
