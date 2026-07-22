package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.data.db.dao.PassbookDao
import com.phuzle.labs.messages.data.db.entity.BankAccountEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * There is no real bank-SMS-to-ledger pipeline in this pass (see PRD Passbook section), so the
 * account/transaction/reminder tables are seeded once with the same sample data the prototype
 * ships with, and read from Room from then on like any other real table.
 */
class PassbookRepository(private val dao: PassbookDao) {

    fun observeAccounts(): Flow<List<BankAccountEntity>> = dao.observeAccounts()
    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun observeReminders(): Flow<List<ReminderEntity>> = dao.observeReminders()

    suspend fun seedIfEmpty() {
        if (dao.accountCount() > 0) return
        val now = System.currentTimeMillis()
        val hour = TimeUnit.HOURS.toMillis(1)
        val day = TimeUnit.DAYS.toMillis(1)

        dao.insertAccounts(
            listOf(
                BankAccountEntity("a1", "Northgate Checking", "4471", "Checking", 348_219),
                BankAccountEntity("a2", "Horizon Rewards Card", "8823", "Credit · \$5,000 limit", 61_240),
                BankAccountEntity("a3", "Summit Savings", "0192", "Savings", 1_290_000),
            )
        )
        dao.insertTransactions(
            listOf(
                TransactionEntity("x1", "Corner Cafe", "8823", -4210, now - 2 * hour, isCredit = false),
                TransactionEntity("x2", "Payroll Deposit", "4471", 240_000, now - day - 9 * hour, isCredit = true),
                TransactionEntity("x3", "Payment Posted", "4471", -120_000, now - 2 * day, isCredit = false),
                TransactionEntity("x4", "Fleet Deliveries", "8823", -1899, now - 2 * day - 4 * hour, isCredit = false),
                TransactionEntity("x5", "Interest Earned", "0192", 1422, now - 3 * day, isCredit = true),
            )
        )
        dao.insertReminders(
            listOf(
                ReminderEntity("r1", "Horizon Card bill due", "\$612.40 minimum due on your Rewards Card", now + 3 * day),
                ReminderEntity("r2", "Confirm dinner with Jordan", "Reply to confirm 7 PM plans", now + 4 * hour),
            )
        )
    }
}
