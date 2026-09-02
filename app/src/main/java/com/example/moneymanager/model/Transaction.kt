package com.example.moneymanager.model

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val category: Category,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
