package com.example.moneymanager.data.mapper

import com.example.moneymanager.data.local.TransactionEntity
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.Transaction
import com.example.moneymanager.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    title = title,
    amount = amount,
    type = TransactionType.valueOf(type),
    category = Category.valueOf(category),
    note = note,
    createdAt = createdAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title.trim(),
    amount = amount,
    type = type.name,
    category = category.name,
    note = note.trim(),
    createdAt = createdAt,
)
