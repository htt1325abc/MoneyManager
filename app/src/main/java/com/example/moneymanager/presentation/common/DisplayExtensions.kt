package com.example.moneymanager.presentation.common

import androidx.annotation.StringRes
import com.example.moneymanager.R
import com.example.moneymanager.model.Category
import com.example.moneymanager.model.TransactionSort
import com.example.moneymanager.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.FOOD -> R.string.category_food
        Category.TRANSPORT -> R.string.category_transport
        Category.SHOPPING -> R.string.category_shopping
        Category.ENTERTAINMENT -> R.string.category_entertainment
        Category.SALARY -> R.string.category_salary
        Category.EDUCATION -> R.string.category_education
        Category.HEALTH -> R.string.category_health
        Category.BILL -> R.string.category_bill
        Category.OTHER -> R.string.category_other
    }

@get:StringRes
val TransactionType.labelRes: Int
    get() = when (this) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
    }

@get:StringRes
val TransactionSort.labelRes: Int
    get() = when (this) {
        TransactionSort.NEWEST -> R.string.sort_newest
        TransactionSort.OLDEST -> R.string.sort_oldest
        TransactionSort.HIGHEST_AMOUNT -> R.string.sort_highest
        TransactionSort.LOWEST_AMOUNT -> R.string.sort_lowest
    }

fun Long.toMoneyFormat(): String = NumberFormat
    .getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    .apply { maximumFractionDigits = 0 }
    .format(this)

fun Long.toDateLabel(): String = SimpleDateFormat(
    "dd/MM/yyyy",
    Locale.forLanguageTag("vi-VN"),
).format(Date(this))
