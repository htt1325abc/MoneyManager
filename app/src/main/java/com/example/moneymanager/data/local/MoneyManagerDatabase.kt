package com.example.moneymanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MoneyManagerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
