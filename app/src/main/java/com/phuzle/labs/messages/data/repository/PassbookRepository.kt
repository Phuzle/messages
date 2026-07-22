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
 * [TransactionEntity.accountLast4]. Reminders have no real source yet (that needs the Layer 2/3
 * AI this pass doesn't implement) so the table stays genuinely empty rather than pre-filled with
 * sample data — the UI shows an honest empty state until a real source exists.
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
}
