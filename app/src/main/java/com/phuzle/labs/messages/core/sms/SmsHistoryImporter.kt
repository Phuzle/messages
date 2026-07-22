package com.phuzle.labs.messages.core.sms

import android.content.Context
import android.provider.Telephony
import com.phuzle.labs.messages.core.contacts.ContactLookup
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.domain.categorization.CategoryClassifier
import com.phuzle.labs.messages.domain.model.AvatarPalette
import com.phuzle.labs.messages.domain.model.Category
import java.util.UUID

/**
 * One-time backfill from the system SMS provider. [SmsDeliverReceiver] only ever sees messages
 * that arrive *after* we hold the default-SMS-app role — anything sent/received earlier (under
 * whichever app used to be default) already lives in `content://sms` and would otherwise never
 * show up here. Gated by [com.phuzle.labs.messages.data.prefs.SettingsRepository]'s
 * `historyImported` flag so it only ever runs once per install.
 */
class SmsHistoryImporter(
    private val context: Context,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val contactLookup: ContactLookup,
    private val classifier: CategoryClassifier,
) {
    suspend fun importAll() {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null,
            null,
            "${Telephony.Sms.DATE} ASC",
        ) ?: return

        cursor.use {
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            if (addressIdx < 0 || bodyIdx < 0 || dateIdx < 0) return

            while (it.moveToNext()) {
                val address = it.getString(addressIdx)?.takeIf { addr -> addr.isNotBlank() } ?: continue
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX

                val thread = findOrTouchThread(address, body, date, outgoing)
                messageDao.insert(MessageEntity(threadId = thread.id, body = body, timestamp = date, outgoing = outgoing))
            }
        }
    }

    private suspend fun findOrTouchThread(address: String, body: String, date: Long, outgoing: Boolean): ThreadEntity {
        val existing = threadDao.findBySender(address)
        if (existing != null) {
            if (date >= existing.lastMessageTime) {
                val updated = existing.copy(lastMessagePreview = body, lastMessageTime = date)
                threadDao.upsert(updated)
                return updated
            }
            return existing
        }
        val contactName = contactLookup.displayNameFor(address)
        val category = if (outgoing) Category.Personal else classifier.classify(address, body)
        val created = ThreadEntity(
            id = "thread-" + UUID.randomUUID(),
            sender = address,
            displayName = contactName ?: address,
            category = category.name,
            isBusiness = contactName == null,
            avatarColor = AvatarPalette.forSeed(address),
            lastMessagePreview = body,
            lastMessageTime = date,
            unread = false,
        )
        threadDao.upsert(created)
        return created
    }
}
