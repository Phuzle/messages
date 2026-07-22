package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phuzle.labs.messages.data.db.entity.BankAccountEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassbookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<BankAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("SELECT COUNT(*) FROM bank_accounts")
    suspend fun accountCount(): Int

    @Query("SELECT * FROM bank_accounts")
    fun observeAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM transactions ORDER BY time DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM reminders ORDER BY dueAt ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>
}
