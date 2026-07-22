package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.data.db.dao.PassbookDao
import com.phuzle.labs.messages.data.db.entity.BankAccountEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Bank accounts and reminders still have no real source (deriving a running balance or a due-date
 * reminder from unstructured SMS text reliably needs the Layer 2/3 AI this pass doesn't implement),
 * so those two tables stay seeded once with the prototype's sample data. The transaction feed is
 * real, though: [recordTransaction] is called from [com.phuzle.labs.messages.core.sms.SmsDeliverReceiver]
 * for every message classified as Transactions, using [com.phuzle.labs.messages.domain.categorization.TransactionExtractor]'s
 * regex-based amount/merchant/last-4 extraction.
 */
class PassbookRepository(private val dao: PassbookDao) {

    fun observeAccounts(): Flow<List<BankAccountEntity>> = dao.observeAccounts()
    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun observeReminders(): Flow<List<ReminderEntity>> = dao.observeReminders()

    suspend fun recordTransaction(merchant: String, accountLast4: String, amountCents: Long, isCredit: Boolean, timestampMillis: Long) {
        dao.insertTransactions(
            listOf(
                TransactionEntity(
                    id = "tx-" + java.util.UUID.randomUUID(),
                    merchant = merchant,
                    accountLast4 = accountLast4,
                    amountCents = amountCents,
                    time = timestampMillis,
                    isCredit = isCredit,
                ),
            ),
        )
    }

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
