package com.example.moneymanager.presentation.common

import androidx.fragment.app.Fragment
import com.example.moneymanager.R
import com.example.moneymanager.model.Transaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Fragment.confirmTransactionDeletion(
    transaction: Transaction,
    onConfirm: () -> Unit,
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_title)
        .setMessage(getString(R.string.delete_message, transaction.title))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
        .show()
}
