package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.core.sms.SmsProviderSync
import com.phuzle.labs.messages.data.db.dao.BlockedNumberDao
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.SearchCandidateRow
import com.phuzle.labs.messages.data.db.dao.SystemLinkedMessageRow
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.dao.ThreadUnreadCount
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
    private val smsProviderSync: SmsProviderSync,
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
    fun observeUnreadCounts(): Flow<List<ThreadUnreadCount>> = messageDao.observeUnreadCounts()

    suspend fun getThread(id: String): ThreadEntity? = threadDao.findById(id)
    suspend fun isBlocked(number: String): Boolean = blockedNumberDao.isBlocked(number)

    data class StorageOverview(val chatCount: Int, val senderCount: Int, val messageCount: Int)

    suspend fun storageOverview(): StorageOverview =
        StorageOverview(threadDao.countActiveThreads(), threadDao.countActiveSenders(), messageDao.countAll())

    /** Re-runs [classify] against every existing thread's (sender, latest message) and updates
     * just its `category` column when it changed — see AppViewModel.reclassifyThreadsIfNeeded and
     * RegexRules.CURRENT_VERSION for why this exists and when it runs. Purely additive: never
     * touches message rows, senders, or anything else, and a no-op write (category unchanged) is
     * skipped so this doesn't spuriously invalidate reactive thread-list queries for every thread
     * on every app launch. */
    suspend fun reclassifyAllThreads(classify: (sender: String, latestBody: String) -> Category) {
        threadDao.getAllOnce().forEach { thread ->
            val recategorized = classify(thread.sender, thread.lastMessagePreview).name
            if (recategorized != thread.category) {
                threadDao.updateCategory(thread.id, recategorized)
            }
        }
    }

    /** AppViewModel.backfillPassbookIfNeeded's source data — every inbound message belonging to a
     * thread already categorized Transactions, paired with that thread (for its displayName as
     * TransactionExtractor's merchant fallback). Installs that ran SmsHistoryImporter before it
     * extracted transactions (see that class's doc comment) never got these into Passbook at all;
     * this lets that one-time backfill happen against already-imported data instead of requiring
     * a re-scan of the system SMS provider (which risks duplicating messages, since insert there
     * has no dedup check). */
    suspend fun transactionCandidateMessages(): List<Pair<ThreadEntity, MessageEntity>> =
        threadDao.getAllOnce()
            .filter { it.category == Category.Transactions.name }
            .flatMap { thread -> messageDao.allForThread(thread.id).filterNot { it.outgoing }.map { thread to it } }

    /** Real SMS_DELIVER path: find-or-create the thread for [sender], then append the message.
     * [subscriptionId] is the SIM this message arrived on, when knowable (see SubscriptionHelper)
     * — stored on the message and remembered on the thread so a later reply defaults to going out
     * via that same SIM. [systemSmsId] is the row this message got when SmsDeliverReceiver wrote
     * it into the system provider — see MessageEntity.systemSmsId. */
    suspend fun recordIncomingMessage(
        sender: String,
        displayName: String,
        isBusiness: Boolean,
        category: Category,
        body: String,
        timestampMillis: Long,
        photoUri: String? = null,
        subscriptionId: Int? = null,
        systemSmsId: Long? = null,
    ): Pair<ThreadEntity, MessageEntity> {
        val existing = threadDao.findBySender(sender)
        val thread = if (existing != null) {
            val updated = existing.copy(
                displayName = displayName,
                photoUri = photoUri,
                lastMessagePreview = body,
                lastMessageOutgoing = false,
                lastMessageTime = timestampMillis,
                unread = true,
                // A reply from a previously-deleted/archived sender surfaces back in the inbox.
                deletedAt = null,
                archived = false,
                preferredSubscriptionId = subscriptionId ?: existing.preferredSubscriptionId,
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
                preferredSubscriptionId = subscriptionId,
            )
            threadDao.upsert(created)
            created
        }
        val message = MessageEntity(
            threadId = thread.id, body = body, timestamp = timestampMillis, outgoing = false, read = false,
            subscriptionId = subscriptionId, systemSmsId = systemSmsId,
        )
        val id = messageDao.insert(message)
        return thread to message.copy(id = id)
    }

    /** Compose / thread-reply path. When [scheduledFor] is set the message is queued, not sent yet.
     * [subscriptionId] is the SIM this was (or will be) sent from, when the caller resolved one. */
    suspend fun composeOutgoingThread(
        to: String,
        body: String,
        scheduledFor: Long?,
        scheduleLabel: String?,
        nowMillis: Long,
        displayName: String = to,
        photoUri: String? = null,
        subscriptionId: Int? = null,
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
            preferredSubscriptionId = subscriptionId,
        ).also { threadDao.upsert(it) }
        return thread to appendOutgoingMessage(thread.id, body, scheduledFor, scheduleLabel, nowMillis, subscriptionId)
    }

    suspend fun appendOutgoingMessage(
        threadId: String,
        body: String,
        scheduledFor: Long?,
        scheduleLabel: String?,
        nowMillis: Long,
        subscriptionId: Int? = null,
    ): MessageEntity {
        val message = MessageEntity(
            threadId = threadId,
            body = body,
            timestamp = nowMillis,
            outgoing = true,
            scheduledFor = scheduledFor,
            scheduleLabel = scheduleLabel,
            sent = scheduledFor == null,
            subscriptionId = subscriptionId,
        )
        val id = messageDao.insert(message)
        val preview = if (scheduledFor != null) "Scheduled for $scheduleLabel" else body
        threadDao.touchLastMessage(threadId, preview, nowMillis, outgoing = true)
        if (subscriptionId != null) threadDao.setPreferredSubscriptionId(threadId, subscriptionId)
        return message.copy(id = id)
    }

    /** Backfills the system provider's row id onto an already-inserted outgoing message, once
     * SmsSender.send() actually performs the write-through insert (which happens after this app's
     * own Room row already exists — sending is deliberately deferred behind the undo window, so
     * the two can't happen in one step the way they do for incoming messages). */
    suspend fun setSystemSmsId(messageId: Long, systemSmsId: Long) = messageDao.setSystemSmsId(messageId, systemSmsId)

    /** Deletes the message and recomputes the thread's cached preview so the inbox never shows a
     * stale last-message after its own last message is removed. Returns the deleted row so the
     * caller can offer a real "undo" by re-inserting it with [restoreMessage]. Also deletes the
     * matching row from the system SMS provider (if any) so the message doesn't keep existing
     * there — see MessageEntity.systemSmsId — though "undo" only restores it here, not there. */
    suspend fun deleteMessage(threadId: String, messageId: Long): MessageEntity? {
        val deleted = messageDao.findById(messageId)
        messageDao.deleteById(messageId)
        refreshLastMessage(threadId)
        deleted?.systemSmsId?.let { smsProviderSync.delete(listOf(it)) }
        return deleted
    }

    /** Reconciliation-only (see AppViewModel.reconcileWithSystemProvider): removes a message
     * purely because its system-provider row is already gone — there is nothing to sync back to
     * the provider here, since that absence is exactly why this is being called. */
    suspend fun deleteReconciledMessage(threadId: String, messageId: Long) {
        messageDao.deleteById(messageId)
        refreshLastMessage(threadId)
    }

    suspend fun restoreMessage(message: MessageEntity) {
        messageDao.insert(message)
        refreshLastMessage(message.threadId)
    }

    suspend fun firstMessageTime(threadId: String): Long? = messageDao.firstMessageTime(threadId)

    /** Contact info's "Clear conversation" — wipes every message but keeps the thread itself.
     * Returns the deleted rows so the caller can offer undo. Also delete-throughs every one of
     * those messages' system-provider rows, same reasoning as [deleteMessage]. */
    suspend fun clearConversation(threadId: String): List<MessageEntity> {
        val all = messageDao.allForThread(threadId)
        messageDao.deleteAllForThread(threadId)
        refreshLastMessage(threadId)
        smsProviderSync.delete(all.mapNotNull { it.systemSmsId })
        return all
    }

    suspend fun restoreMessages(messages: List<MessageEntity>) {
        messages.forEach { messageDao.insert(it) }
        messages.firstOrNull()?.let { refreshLastMessage(it.threadId) }
    }

    private suspend fun refreshLastMessage(threadId: String) {
        val latest = messageDao.latestForThread(threadId)
        threadDao.touchLastMessage(
            threadId, latest?.body ?: "No messages", latest?.timestamp ?: System.currentTimeMillis(),
            outgoing = latest?.outgoing ?: false,
        )
    }

    suspend fun latestIncomingOtpMessage(): MessageEntity? = messageDao.latestIncomingOtpMessage()
    suspend fun dueScheduledMessages(now: Long) = messageDao.dueScheduled(now)
    suspend fun markMessageSent(messageId: Long, sentAt: Long) = messageDao.markSent(messageId, sentAt)

    /** [currentlyUnread] true means the call is transitioning the thread TO read — in that case
     * every message in it is marked read too, not just the thread-level flag, so the numbered
     * unread badge clears along with the dot. Going the other way (marking unread) has no single
     * message to un-read, so only the thread-level flag flips; see ThreadUi's fallback display.
     * Also write-throughs the read state to the system SMS provider so switching back to another
     * SMS app (or anything else reading that shared table) doesn't show these as unread again. */
    suspend fun toggleRead(id: String, currentlyUnread: Boolean) {
        threadDao.setUnread(id, !currentlyUnread)
        if (currentlyUnread) {
            val systemIds = messageDao.unreadSystemSmsIdsForThread(id)
            messageDao.markThreadRead(id)
            smsProviderSync.markRead(systemIds)
        }
    }

    suspend fun markAllRead() {
        val systemIds = messageDao.allUnreadSystemSmsIds()
        threadDao.markAllRead()
        messageDao.markAllRead()
        smsProviderSync.markRead(systemIds)
    }

    /** The overflow menu's "Mark all as read" actually calls this, not [markAllRead] — [threadIds]
     * is exactly what the dashboard's current category/unread-only filter shows, so this can never
     * silently reach into a category the user isn't even looking at. [markAllRead] itself is kept
     * for whatever legitimately wants every thread regardless of filter (there's no such caller
     * today, but scoping is the caller's job, not something to bake into "read every message"). */
    suspend fun markThreadsRead(threadIds: List<String>) {
        if (threadIds.isEmpty()) return
        val systemIds = messageDao.unreadSystemSmsIdsForThreads(threadIds)
        threadDao.markThreadsRead(threadIds)
        messageDao.markThreadsRead(threadIds)
        smsProviderSync.markRead(systemIds)
    }
    suspend fun archive(id: String) = threadDao.setArchived(id, true)
    suspend fun unarchive(id: String) = threadDao.setArchived(id, false)
    suspend fun setPrivate(id: String, isPrivate: Boolean) = threadDao.setPrivate(id, isPrivate)
    suspend fun softDelete(id: String, whenMillis: Long) = threadDao.setDeletedAt(id, whenMillis)
    suspend fun restore(id: String) = threadDao.setDeletedAt(id, null)

    /** The 30-day auto-purge worker. Gathers every about-to-be-purged thread's messages'
     * systemSmsIds *before* the delete (Room's cascade wipes the message rows along with the
     * thread), then delete-throughs them so purging here doesn't leave them behind forever in
     * the system provider. */
    suspend fun purgeDeletedBefore(cutoffMillis: Long) {
        val threadIds = threadDao.deletedThreadIdsBefore(cutoffMillis)
        val systemIds = messageDao.systemSmsIdsForThreads(threadIds)
        threadDao.purgeDeletedBefore(cutoffMillis)
        smsProviderSync.delete(systemIds)
    }

    /** Recycle bin's "Empty" — same delete-through reasoning as [purgeDeletedBefore], for a single
     * user-picked thread instead of everything past the 30-day cutoff. */
    suspend fun hardDelete(id: String) {
        val systemIds = messageDao.systemSmsIdsForThread(id)
        threadDao.deleteById(id)
        smsProviderSync.delete(systemIds)
    }

    fun observeSearchCandidates(): Flow<List<SearchCandidateRow>> = threadDao.observeSearchCandidates()
    suspend fun purgeOtpMessagesBefore(cutoffMillis: Long) = messageDao.purgeOtpMessagesBefore(cutoffMillis)

    suspend fun block(number: String) = blockedNumberDao.block(BlockedNumberEntity(number))
    suspend fun unblock(number: String) = blockedNumberDao.unblock(BlockedNumberEntity(number))

    // region ---- system SMS provider reconciliation (see AppViewModel.reconcileWithSystemProvider) ----

    suspend fun messagesLinkedToSystemProvider(): List<SystemLinkedMessageRow> = messageDao.allWithSystemSmsId()
    suspend fun setMessageReadState(messageId: Long, read: Boolean) = messageDao.setReadState(messageId, read)
    suspend fun refreshThreadUnreadFlag(threadId: String) = threadDao.setUnread(threadId, messageDao.unreadCountForThread(threadId) > 0)

    // endregion
}
