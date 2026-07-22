package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.data.db.dao.PassbookDao
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Transactions are entirely real: [recordTransaction] is called from
 * [com.phuzle.labs.messages.core.sms.SmsDeliverReceiver] for every message classified as
 * Transactions, using [com.phuzle.labs.messages.domain.categorization.TransactionExtractor]'s
 * regex-based amount/merchant/last-4 extraction — nothing here is seeded or fabricated.
 * "Accounts" aren't a separate stored concept; the UI derives them by grouping transactions by
 * [TransactionEntity.accountLast4]. Reminders are now real too, via [insertReminder] — the
 * server/'s Layer 3 reminder-extraction endpoint, called from SmsDeliverReceiver when cloud
 * fallback is enabled — nothing here is seeded or fabricated.
 */
class PassbookRepository(private val dao: PassbookDao) {

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

    suspend fun insertReminder(title: String, detail: String, dueAtEpochMillis: Long) {
        dao.insertReminders(listOf(ReminderEntity(id = "reminder-" + java.util.UUID.randomUUID(), title = title, detail = detail, dueAt = dueAtEpochMillis)))
    }

    suspend fun findReminder(id: String): ReminderEntity? = dao.findReminder(id)
    suspend fun deleteReminder(id: String) = dao.deleteReminder(id)
    suspend fun restoreReminder(reminder: ReminderEntity) = dao.insertReminders(listOf(reminder))
}
