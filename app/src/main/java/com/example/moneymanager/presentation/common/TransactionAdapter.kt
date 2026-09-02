package com.example.moneymanager.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymanager.R
import com.example.moneymanager.databinding.ItemTransactionBinding
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionType

class TransactionAdapter(
    private val onEdit: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit,
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) = with(binding) {
            val context = root.context
            val isIncome = transaction.type == TransactionType.INCOME
            val sign = if (isIncome) "+" else "−"

            titleText.text = transaction.title
            amountText.text = context.getString(R.string.signed_amount, sign, transaction.amount.toMoneyFormat())
            amountText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isIncome) R.color.ledger_income else R.color.ledger_expense,
                )
            )
            metaText.text = context.getString(
                R.string.transaction_meta,
                context.getString(transaction.category.labelRes),
                transaction.createdAt.toDateLabel(),
            )
            noteText.text = transaction.note
            noteText.isVisible = transaction.note.isNotBlank()
            editButton.setOnClickListener { onEdit(transaction) }
            deleteButton.setOnClickListener { onDelete(transaction) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem == newItem
    }
}
