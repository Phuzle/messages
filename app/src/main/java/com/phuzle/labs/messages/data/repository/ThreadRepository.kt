package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.data.db.dao.BlockedNumberDao
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.domain.model.AvatarPalette
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.flow.Flow

class ThreadRepository(
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val blockedNumberDao: BlockedNumberDao,
) {
    fun observeInbox(): Flow<List<ThreadEntity>> = threadDao.observeInbox()
    fun observeAllActive(): Flow<List<ThreadEntity>> = threadDao.observeAllActive()
    fun observeArchived(): Flow<List<ThreadEntity>> = threadDao.observeArchived()
    fun observeDeleted(): Flow<List<ThreadEntity>> = threadDao.observeDeleted()
    fun observePrivate(): Flow<List<ThreadEntity>> = threadDao.observePrivate()
    fun observeMessages(threadId: String): Flow<List<MessageEntity>> = messageDao.observeForThread(threadId)
    fun observeRecentMessages(threadId: String, limit: Int): Flow<List<MessageEntity>> = messageDao.observeRecentForThread(threadId, limit)
    suspend fun olderMessagesThan(threadId: String, beforeTimestamp: Long, limit: Int): List<MessageEntity> =
        messageDao.olderThan(threadId, beforeTimestamp, limit)
    fun observeBlockedNumbers(): Flow<List<BlockedNumberEntity>> = blockedNumberDao.observeAll()

    suspend fun getThread(id: String): ThreadEntity? = threadDao.findById(id)
    suspend fun isBlocked(number: String): Boolean = blockedNumberDao.isBlocked(number)

    /** Real SMS_DELIVER path: find-or-create the thread for [sender], then append the message. */
    suspend fun recordIncomingMessage(
        sender: String,
        displayName: String,
        isBusiness: Boolean,
        category: Category,
        body: String,
        timestampMillis: Long,
        photoUri: String? = null,
    ): Pair<ThreadEntity, MessageEntity> {
        val existing = threadDao.findBySender(sender)
        val thread = if (existing != null) {
            val updated = existing.copy(
                displayName = displayName,
                photoUri = photoUri,
                lastMessagePreview = body,
                lastMessageTime = timestampMillis,
                unread = true,
                // A reply from a previously-deleted/archived sender surfaces back in the inbox.
                deletedAt = null,
                archived = false,
            )
            // @Update, not upsert()/INSERT-OR-REPLACE: REPLACE deletes-then-reinserts the
            // conflicting row, which cascades onDelete=CASCADE and wipes every message this
            // thread already had. Plain UPDATE touches only this row.
            threadDao.update(updated)
            updated
        } else {
            val created = ThreadEntity(
                id = "thread-" + java.util.UUID.randomUUID(),
                sender = sender,
                displayName = displayName,
                category = category.name,
                isBusiness = isBusiness,
                avatarColor = AvatarPalette.forSeed(sender),
                photoUri = photoUri,
                lastMessagePreview = body,
                lastMessageTime = timestampMillis,
                unread = true,
            )
            threadDao.upsert(created)
            created
        }
        val message = MessageEntity(threadId = thread.id, body = body, timestamp = timestampMillis, outgoing = false)
        val id = messageDao.insert(message)
        return thread to message.copy(id = id)
    }

    /** Compose / thread-reply path. When [scheduledFor] is set the message is queued, not sent yet. */
    suspend fun composeOutgoingThread(
        to: String,
        body: String,
        scheduledFor: Long?,
        scheduleLabel: String?,
        nowMillis: Long,
        displayName: String = to,
        photoUri: String? = null,
    ): Pair<ThreadEntity, MessageEntity> {
        val existing = threadDao.findBySender(to)
        val thread = existing ?: ThreadEntity(
            id = "thread-" + java.util.UUID.randomUUID(),
            sender = to,
            displayName = displayName,
            category = Category.Personal.name,
            isBusiness = false,
            avatarColor = AvatarPalette.forSeed(to),
            photoUri = photoUri,
            lastMessagePreview = body,
            lastMessageTime = nowMillis,
            unread = false,
        ).also { threadDao.upsert(it) }
        return thread to appendOutgoingMessage(thread.id, body, scheduledFor, scheduleLabel, nowMillis)
    }

    suspend fun appendOutgoingMessage(
        threadId: String,
        body: String,
        scheduledFor: Long?,
        scheduleLabel: String?,
        nowMillis: Long,
    ): MessageEntity {
        val message = MessageEntity(
            threadId = threadId,
            body = body,
            timestamp = nowMillis,
            outgoing = true,
            scheduledFor = scheduledFor,
            scheduleLabel = scheduleLabel,
            sent = scheduledFor == null,
        )
        val id = messageDao.insert(message)
        val preview = if (scheduledFor != null) "Scheduled for $scheduleLabel" else body
        threadDao.touchLastMessage(threadId, preview, nowMillis)
        return message.copy(id = id)
    }

    /** Deletes the message and recomputes the thread's cached preview so the inbox never shows a
     * stale last-message after its own last message is removed. Returns the deleted row so the
     * caller can offer a real "undo" by re-inserting it with [restoreMessage]. */
    suspend fun deleteMessage(threadId: String, messageId: Long): MessageEntity? {
        val deleted = messageDao.findById(messageId)
        messageDao.deleteById(messageId)
        refreshLastMessage(threadId)
        return deleted
    }

    suspend fun restoreMessage(message: MessageEntity) {
        messageDao.insert(message)
        refreshLastMessage(message.threadId)
    }

    private suspend fun refreshLastMessage(threadId: String) {
        val latest = messageDao.latestForThread(threadId)
        threadDao.touchLastMessage(threadId, latest?.body ?: "No messages", latest?.timestamp ?: System.currentTimeMillis())
    }

    suspend fun latestIncomingOtpMessage(): MessageEntity? = messageDao.latestIncomingOtpMessage()
    suspend fun dueScheduledMessages(now: Long) = messageDao.dueScheduled(now)
    suspend fun markMessageSent(messageId: Long, sentAt: Long) = messageDao.markSent(messageId, sentAt)

    suspend fun toggleRead(id: String, currentlyUnread: Boolean) = threadDao.setUnread(id, !currentlyUnread)
    suspend fun markAllRead() = threadDao.markAllRead()
    suspend fun archive(id: String) = threadDao.setArchived(id, true)
    suspend fun unarchive(id: String) = threadDao.setArchived(id, false)
    suspend fun setPrivate(id: String, isPrivate: Boolean) = threadDao.setPrivate(id, isPrivate)
    suspend fun softDelete(id: String, whenMillis: Long) = threadDao.setDeletedAt(id, whenMillis)
    suspend fun restore(id: String) = threadDao.setDeletedAt(id, null)
    suspend fun purgeDeletedBefore(cutoffMillis: Long) = threadDao.purgeDeletedBefore(cutoffMillis)
    suspend fun hardDelete(id: String) = threadDao.deleteById(id)
    fun searchInboxIds(query: String): Flow<List<String>> = threadDao.searchInboxIds(query)
    suspend fun purgeOtpMessagesBefore(cutoffMillis: Long) = messageDao.purgeOtpMessagesBefore(cutoffMillis)

    suspend fun block(number: String) = blockedNumberDao.block(BlockedNumberEntity(number))
    suspend fun unblock(number: String) = blockedNumberDao.unblock(BlockedNumberEntity(number))
}
