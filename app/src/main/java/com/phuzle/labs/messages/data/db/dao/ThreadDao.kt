package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(thread: ThreadEntity)

    @Update
    suspend fun update(thread: ThreadEntity)

    @Query("SELECT * FROM threads WHERE deletedAt IS NULL AND archived = 0 AND isPrivate = 0 ORDER BY lastMessageTime DESC")
    fun observeInbox(): Flow<List<ThreadEntity>>

    /** Every non-deleted thread regardless of archived/private, used to resolve whichever thread is open. */
    @Query("SELECT * FROM threads WHERE deletedAt IS NULL")
    fun observeAllActive(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE deletedAt IS NULL AND archived = 1 ORDER BY lastMessageTime DESC")
    fun observeArchived(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE deletedAt IS NULL AND isPrivate = 1 ORDER BY lastMessageTime DESC")
    fun observePrivate(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ThreadEntity?

    /** For Storage & Data's overview — active (non-deleted) chats and distinct senders among them. */
    @Query("SELECT COUNT(*) FROM threads WHERE deletedAt IS NULL")
    suspend fun countActiveThreads(): Int

    @Query("SELECT COUNT(DISTINCT sender) FROM threads WHERE deletedAt IS NULL")
    suspend fun countActiveSenders(): Int

    @Query("SELECT * FROM threads WHERE sender = :sender LIMIT 1")
    suspend fun findBySender(sender: String): ThreadEntity?

    @Query("UPDATE threads SET unread = :unread WHERE id = :id")
    suspend fun setUnread(id: String, unread: Boolean)

    @Query("UPDATE threads SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE threads SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivate(id: String, isPrivate: Boolean)

    @Query("UPDATE threads SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?)

    @Query("UPDATE threads SET lastMessagePreview = :preview, lastMessageTime = :time WHERE id = :id")
    suspend fun touchLastMessage(id: String, preview: String, time: Long)

    @Query("UPDATE threads SET unread = 0 WHERE deletedAt IS NULL")
    suspend fun markAllRead()

    @Query("DELETE FROM threads WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    /** Permanent delete (e.g. "Empty recycle bin") — messages cascade via the foreign key. */
    @Query("DELETE FROM threads WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Every (thread, message) pairing eligible for search — fuzzy-scored in Kotlin (see
     * FuzzyMatcher) against the sender's name *and* every message in the thread's full history,
     * not just the cached last-message preview. Loaded only while a search is active (see
     * AppViewModel's searchMatchingIds), not on every recomposition. */
    @Query(
        """
        SELECT t.id AS threadId, t.displayName AS displayName, m.body AS body
        FROM threads t
        LEFT JOIN messages m ON m.threadId = t.id
        WHERE t.deletedAt IS NULL AND t.archived = 0 AND t.isPrivate = 0
        """
    )
    fun observeSearchCandidates(): Flow<List<SearchCandidateRow>>
}

data class SearchCandidateRow(val threadId: String, val displayName: String, val body: String?)
