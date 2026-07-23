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

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeForThread(threadId: String): Flow<List<MessageEntity>>

    /**
     * The reactive "live window": only the most recent [limit] messages, so opening a thread with
     * years of history doesn't pull every row into memory at once. Ordered DESC here purely so
     * `LIMIT` keeps the *newest* rows — callers reverse it back to chronological order.
     */
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentForThread(threadId: String, limit: Int): Flow<List<MessageEntity>>

    /** One-shot "load older" page, keyed off the oldest timestamp currently held in memory. */
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun olderThan(threadId: String, beforeTimestamp: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sent = 0 AND scheduledFor <= :now")
    suspend fun dueScheduled(now: Long): List<MessageEntity>

    @Query("UPDATE messages SET sent = 1, timestamp = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MessageEntity?

    /** Used to recompute a thread's cached preview/time after its last message is deleted. */
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp DESC LIMIT 1")
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

    @Query("DELETE FROM messages WHERE timestamp < :cutoff AND threadId IN (SELECT id FROM threads WHERE category = 'Otp')")
    suspend fun purgeOtpMessagesBefore(cutoff: Long)

    /** Drives the 30-second OTP hot-swap modal on app foreground. */
    @Query(
        "SELECT m.* FROM messages m JOIN threads t ON m.threadId = t.id " +
            "WHERE t.category = 'Otp' AND m.outgoing = 0 ORDER BY m.timestamp DESC LIMIT 1"
    )
    suspend fun latestIncomingOtpMessage(): MessageEntity?
}
