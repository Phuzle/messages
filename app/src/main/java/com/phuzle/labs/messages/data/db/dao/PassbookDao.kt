package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassbookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("SELECT * FROM transactions ORDER BY time DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM reminders ORDER BY dueAt ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun findReminder(id: String): ReminderEntity?

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    /** Used by DriveBackupMerger to union-by-id instead of blindly re-inserting. */
    @Query("SELECT id FROM transactions")
    suspend fun allTransactionIds(): List<String>

    /** PassbookRepository.recordTransaction's dedup check — (merchant, accountLast4, amountCents,
     * time) stands in for a natural key here, the same idea as MessageDao.countMatching, so
     * calling recordTransaction twice for what is genuinely the same real-world transaction (e.g.
     * AppViewModel.backfillPassbookIfNeeded re-scanning messages the live receiver already
     * recorded) is a safe no-op instead of a duplicate row. */
    @Query("SELECT COUNT(*) FROM transactions WHERE merchant = :merchant AND accountLast4 = :accountLast4 AND amountCents = :amountCents AND time = :time")
    suspend fun countMatchingTransaction(merchant: String, accountLast4: String, amountCents: Long, time: Long): Int

    @Query("SELECT id FROM reminders")
    suspend fun allReminderIds(): List<String>
}
