package com.phuzle.labs.messages.core.sms

import android.content.Context
import android.provider.Telephony
import com.phuzle.labs.messages.core.contacts.ContactLookup
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.repository.PassbookRepository
import com.phuzle.labs.messages.domain.categorization.CategoryClassifier
import com.phuzle.labs.messages.domain.categorization.RegexRules
import com.phuzle.labs.messages.domain.categorization.TransactionExtractor
import com.phuzle.labs.messages.domain.model.AvatarPalette
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * One-time backfill from the system SMS provider. [SmsDeliverReceiver] only ever sees messages
 * that arrive *after* we hold the default-SMS-app role — anything sent/received earlier (under
 * whichever app used to be default) already lives in `content://sms` and would otherwise never
 * show up here. Gated by [com.phuzle.labs.messages.data.prefs.SettingsRepository]'s
 * `historyImported` flag so it only ever runs once per install.
 *
 * Everything here — the cursor scan and the per-new-sender [ContactLookup] query — is genuinely
 * blocking I/O, so the whole thing runs on [Dispatchers.IO]; running it on the caller's dispatcher
 * (as an earlier version of this did) would freeze the UI for as long as the import takes on a
 * phone with a lot of history.
 *
 * Also runs [TransactionExtractor] on every message this classifies as Transactions, same as
 * [SmsDeliverReceiver] does for live messages — without this, Passbook would stay completely
 * empty after any reset/reinstall/Drive-restore despite the inbox itself being full of bank/card
 * texts, since this backfill used to be the *only* path that never fed Passbook at all. Reminders
 * are deliberately NOT backfilled here: that extraction is Layer 3 (cloud fallback, currently
 * hidden/opt-in — see AppContainer.cloudClassifierClient), and firing one network call per
 * historical message during a bulk import would be slow, costly, and untested at that volume; a
 * user with cloud fallback on will still get reminders for anything that arrives from here on.
 */
class SmsHistoryImporter(
    private val context: Context,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val contactLookup: ContactLookup,
    private val classifier: CategoryClassifier,
    private val passbookRepository: PassbookRepository,
    private val regexRules: RegexRules,
) {
    suspend fun importAll(onProgress: (done: Int, total: Int) -> Unit) = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ),
            null,
            null,
            "${Telephony.Sms.DATE} ASC",
        ) ?: return@withContext

        cursor.use {
            val total = it.count
            onProgress(0, total)
            val idIdx = it.getColumnIndex(Telephony.Sms._ID)
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            val readIdx = it.getColumnIndex(Telephony.Sms.READ)
            if (addressIdx < 0 || bodyIdx < 0 || dateIdx < 0) return@withContext

            var done = 0
            while (it.moveToNext()) {
                val address = it.getString(addressIdx)?.takeIf { addr -> addr.isNotBlank() }
                if (address == null) {
                    done++
                    continue
                }
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                // Outgoing messages are never "unread" regardless of what the system row says —
                // READ on a sent message means something different there (delivery/seen-by-us
                // bookkeeping) and has no bearing on this app's inbound-unread concept.
                val isUnread = !outgoing && readIdx >= 0 && it.getInt(readIdx) == 0
                // Correlates this row back to the system provider (see MessageEntity.systemSmsId)
                // so read/delete actions taken here from now on can write through to it, and so
                // later reconciliation passes can tell "still there" from "deleted elsewhere".
                val systemSmsId = if (idIdx >= 0) it.getLong(idIdx) else null

                val thread = findOrTouchThread(address, body, date, outgoing, isUnread)
                messageDao.insert(
                    MessageEntity(
                        threadId = thread.id, body = body, timestamp = date, outgoing = outgoing, read = !isUnread,
                        systemSmsId = systemSmsId,
                    ),
                )

                // Mirrors SmsDeliverReceiver's per-message classify-then-extract — a thread's
                // stored category only ever reflects its first message (see findOrTouchThread), so
                // this re-classifies each row independently rather than trusting the thread's label,
                // exactly like the live receiver does.
                if (!outgoing && classifier.classify(address, body) == Category.Transactions) {
                    TransactionExtractor.extract(body, regexRules.amountPattern, fallbackMerchant = thread.displayName)?.let { tx ->
                        passbookRepository.recordTransaction(
                            merchant = tx.merchant,
                            accountLast4 = tx.accountLast4,
                            amountCents = tx.amountCents,
                            isCredit = tx.isCredit,
                            timestampMillis = date,
                        )
                    }
                }

                done++
                // Reporting on every row would itself flood the UI with state updates on a large
                // import; a coarse throttle keeps the progress screen smooth without losing accuracy.
                if (done % 20 == 0 || done == total) onProgress(done, total)
            }
        }
    }

    private suspend fun findOrTouchThread(address: String, body: String, date: Long, outgoing: Boolean, isUnread: Boolean): ThreadEntity {
        val existing = threadDao.findBySender(address)
        if (existing != null) {
            // Rows arrive in ascending date order, so a thread's unread flag needs to accumulate
            // across every message seen for it so far, not just reflect whichever happened to be
            // processed last — a thread with any unread message anywhere in its history should
            // end up unread, matching what the system SMS provider itself considered true.
            val stillUnread = existing.unread || isUnread
            if (date >= existing.lastMessageTime) {
                val updated = existing.copy(lastMessagePreview = body, lastMessageOutgoing = outgoing, lastMessageTime = date, unread = stillUnread)
                // @Update, not upsert()/INSERT-OR-REPLACE: REPLACE deletes-then-reinserts the
                // conflicting row, which cascades onDelete=CASCADE and wipes every message
                // already imported for this thread. Plain UPDATE touches only this row.
                threadDao.update(updated)
                return updated
            }
            if (stillUnread != existing.unread) {
                val updated = existing.copy(unread = stillUnread)
                threadDao.update(updated)
                return updated
            }
            return existing
        }
        val contactName = contactLookup.displayNameFor(address)
        val photoUri = if (contactName != null) contactLookup.photoUriFor(address) else null
        val category = if (outgoing) Category.Personal else classifier.classify(address, body)
        val created = ThreadEntity(
            id = "thread-" + UUID.randomUUID(),
            sender = address,
            displayName = contactName ?: address,
            category = category.name,
            isBusiness = contactName == null,
            avatarColor = AvatarPalette.forSeed(address),
            photoUri = photoUri,
            lastMessagePreview = body,
            lastMessageOutgoing = outgoing,
            lastMessageTime = date,
            unread = isUnread,
        )
        threadDao.upsert(created)
        return created
    }
}
