package com.example.moneymanager

import android.app.Application
import androidx.room.Room
import com.example.moneymanager.data.local.MoneyManagerDatabase
import com.example.moneymanager.data.repository.TransactionRepositoryImpl
import com.example.moneymanager.domain.repository.TransactionRepository

class MoneyManagerApplication : Application() {
    private val database: MoneyManagerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            MoneyManagerDatabase::class.java,
            "money-manager.db",
        ).build()
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }
}
