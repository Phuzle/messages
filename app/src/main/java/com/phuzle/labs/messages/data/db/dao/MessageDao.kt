package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL ORDER BY timestamp ASC")
    fun observeForThread(threadId: String): Flow<List<MessageEntity>>

    /**
     * The reactive "live window": only the most recent [limit] messages, so opening a thread with
     * years of history doesn't pull every row into memory at once. Ordered DESC here purely so
     * `LIMIT` keeps the *newest* rows — callers reverse it back to chronological order.
     */
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentForThread(threadId: String, limit: Int): Flow<List<MessageEntity>>

    /** One-shot "load older" page, keyed off the oldest timestamp currently held in memory. */
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun olderThan(threadId: String, beforeTimestamp: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sent = 0 AND scheduledFor <= :now")
    suspend fun dueScheduled(now: Long): List<MessageEntity>

    /** Every not-yet-sent scheduled message regardless of whether its time has passed — the
     * Scheduled Messages hub's whole list, and also what a boot receiver needs to re-arm exact
     * alarms with (AlarmManager forgets every alarm across a reboot; the database is the only
     * durable record of what was still pending). */
    @Query("SELECT * FROM messages WHERE sent = 0 AND scheduledFor IS NOT NULL ORDER BY scheduledFor ASC")
    suspend fun allPendingScheduled(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sent = 0 AND scheduledFor IS NOT NULL ORDER BY scheduledFor ASC")
    fun observePendingScheduled(): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET sent = 1, timestamp = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MessageEntity?

    /** Storage & Data's overview total. */
    @Query("SELECT COUNT(*) FROM messages WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    /** Used to recompute a thread's cached preview/time after its last message is deleted. */
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestForThread(threadId: String): MessageEntity?

    /** Contact-info page's "First contact" row. */
    @Query("SELECT MIN(timestamp) FROM messages WHERE threadId = :threadId")
    suspend fun firstMessageTime(threadId: String): Long?

    /** "Clear conversation" — returns the deleted rows first so the caller can offer undo. */
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun allForThread(threadId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteAllForThread(threadId: String)

    /** DriveBackupMerger's de-dup check — messages have no natural key across independent
     * installs (autoGenerate ids collide meaninglessly), so (threadId, body, timestamp, outgoing)
     * stands in for one: same thread, same text, same instant, same direction is the same message. */
    @Query("SELECT COUNT(*) FROM messages WHERE threadId = :threadId AND body = :body AND timestamp = :timestamp AND outgoing = :outgoing")
    suspend fun countMatching(threadId: String, body: String, timestamp: Long, outgoing: Boolean): Int

    /** Soft-delete only (see MessageEntity.deletedAt) — moves expired OTP messages into the
     * recycle bin instead of destroying them outright, same as every other delete in this app.
     * [purgeSoftDeletedBefore] is what removes them for good, 30 days later. */
    @Query(
        "UPDATE messages SET deletedAt = :now WHERE timestamp < :cutoff AND deletedAt IS NULL " +
            "AND threadId IN (SELECT id FROM threads WHERE category = 'Otp')"
    )
    suspend fun purgeOtpMessagesBefore(cutoff: Long, now: Long)

    /** Recycle Bin's "Deleted OTP codes" section — soft-deleted OTP messages, newest-deleted
     * first, joined with the thread for a sender name to display since the message row alone
     * doesn't carry one. */
    @Query(
        "SELECT m.id AS id, m.threadId AS threadId, t.displayName AS senderName, m.body AS body, " +
            "m.timestamp AS timestamp, m.deletedAt AS deletedAt FROM messages m JOIN threads t ON m.threadId = t.id " +
            "WHERE m.deletedAt IS NOT NULL ORDER BY m.deletedAt DESC"
    )
    fun observeDeletedOtpMessages(): Flow<List<DeletedOtpMessageRow>>

    @Query("UPDATE messages SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreDeletedMessage(id: Long)

    /** The undo half of [restoreDeletedMessage] — re-deletes one message by id, for "Restore all"'s
     * own undo bar (see AppViewModel.restoreAllDeleted). */
    @Query("UPDATE messages SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMessage(id: Long, deletedAt: Long)

    /** The 30-day auto-purge worker's other half — see RecycleBinPurgeWorker. Same cutoff as
     * ThreadDao.purgeDeletedBefore, just for individually soft-deleted messages instead of whole
     * threads. */
    @Query("DELETE FROM messages WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeSoftDeletedBefore(cutoff: Long)

    /** Drives the 30-second OTP hot-swap modal on app foreground. */
    @Query(
        "SELECT m.* FROM messages m JOIN threads t ON m.threadId = t.id " +
            "WHERE t.category = 'Otp' AND m.outgoing = 0 AND m.deletedAt IS NULL ORDER BY m.timestamp DESC LIMIT 1"
    )
    suspend fun latestIncomingOtpMessage(): MessageEntity?

    /** Per-thread unread *message* counts (not just the thread-level unread flag) — the numbered
     * badge on each dashboard row. Only inbound messages count; an outgoing message is never
     * "unread". */
    @Query("SELECT threadId, COUNT(*) AS count FROM messages WHERE outgoing = 0 AND read = 0 AND deletedAt IS NULL GROUP BY threadId")
    fun observeUnreadCounts(): Flow<List<ThreadUnreadCount>>

    /** Opening a thread (or explicitly marking one read) reads every message in it, not just the
     * latest — otherwise the numbered badge would still show older unread messages the user just
     * scrolled past. */
    @Query("UPDATE messages SET read = 1 WHERE threadId = :threadId AND outgoing = 0")
    suspend fun markThreadRead(threadId: String)

    /** Companion to ThreadDao.markAllRead() for the overflow menu's "Mark all as read". */
    @Query("UPDATE messages SET read = 1 WHERE outgoing = 0")
    suspend fun markAllRead()

    /** Scoped variant of [markAllRead] — see ThreadDao.markThreadsRead for why this is scoped to
     * specific thread ids rather than every message in the database. */
    @Query("UPDATE messages SET read = 1 WHERE threadId IN (:threadIds) AND outgoing = 0")
    suspend fun markThreadsRead(threadIds: List<String>)

    // region ---- system SMS provider sync (see SmsProviderSync / MessageEntity.systemSmsId) ----

    @Query("UPDATE messages SET systemSmsId = :systemSmsId WHERE id = :id")
    suspend fun setSystemSmsId(id: Long, systemSmsId: Long)

    @Query("SELECT systemSmsId FROM messages WHERE id = :id AND systemSmsId IS NOT NULL")
    suspend fun systemSmsIdForMessage(id: Long): Long?

    @Query("SELECT systemSmsId FROM messages WHERE threadId = :threadId AND systemSmsId IS NOT NULL")
    suspend fun systemSmsIdsForThread(threadId: String): List<Long>

    @Query("SELECT systemSmsId FROM messages WHERE threadId IN (:threadIds) AND systemSmsId IS NOT NULL")
    suspend fun systemSmsIdsForThreads(threadIds: List<String>): List<Long>

    @Query("SELECT systemSmsId FROM messages WHERE threadId = :threadId AND outgoing = 0 AND read = 0 AND systemSmsId IS NOT NULL")
    suspend fun unreadSystemSmsIdsForThread(threadId: String): List<Long>

    @Query("SELECT systemSmsId FROM messages WHERE outgoing = 0 AND read = 0 AND systemSmsId IS NOT NULL")
    suspend fun allUnreadSystemSmsIds(): List<Long>

    @Query("SELECT systemSmsId FROM messages WHERE threadId IN (:threadIds) AND outgoing = 0 AND read = 0 AND systemSmsId IS NOT NULL")
    suspend fun unreadSystemSmsIdsForThreads(threadIds: List<String>): List<Long>

    @Query("UPDATE messages SET read = :read WHERE id = :id")
    suspend fun setReadState(id: Long, read: Boolean)

    @Query("SELECT COUNT(*) FROM messages WHERE threadId = :threadId AND outgoing = 0 AND read = 0 AND deletedAt IS NULL")
    suspend fun unreadCountForThread(threadId: String): Int

    /** Every message that has a system-provider counterpart — reconciliation's starting point
     * (see AppViewModel.reconcileWithSystemProvider). Rows without one (systemSmsId IS NULL, e.g.
     * a provider write that failed) have nothing to diff against and are correctly excluded. */
    @Query("SELECT id, threadId, systemSmsId, read, outgoing FROM messages WHERE systemSmsId IS NOT NULL")
    suspend fun allWithSystemSmsId(): List<SystemLinkedMessageRow>

    // endregion
}

data class ThreadUnreadCount(val threadId: String, val count: Int)

data class SystemLinkedMessageRow(val id: Long, val threadId: String, val systemSmsId: Long, val read: Boolean, val outgoing: Boolean)

data class DeletedOtpMessageRow(val id: Long, val threadId: String, val senderName: String, val body: String, val timestamp: Long, val deletedAt: Long)
