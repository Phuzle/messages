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

    /** One-shot (not a Flow) snapshot for AppViewModel.reclassifyThreadsIfNeeded — every thread
     * regardless of archived/private/deleted, since a sender's category shouldn't depend on
     * where the user happens to have filed the conversation. */
    @Query("SELECT * FROM threads")
    suspend fun getAllOnce(): List<ThreadEntity>

    @Query("UPDATE threads SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: String, category: String)

    @Query("UPDATE threads SET unread = :unread WHERE id = :id")
    suspend fun setUnread(id: String, unread: Boolean)

    @Query("UPDATE threads SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE threads SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivate(id: String, isPrivate: Boolean)

    @Query("UPDATE threads SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?)

    @Query("UPDATE threads SET lastMessagePreview = :preview, lastMessageTime = :time, lastMessageOutgoing = :outgoing WHERE id = :id")
    suspend fun touchLastMessage(id: String, preview: String, time: Long, outgoing: Boolean)

    @Query("UPDATE threads SET unread = 0 WHERE deletedAt IS NULL")
    suspend fun markAllRead()

    /** Scoped variant of [markAllRead] for the overflow menu's "Mark all as read" — [ids] is
     * whatever the dashboard's current category/unread filter actually shows, so this only ever
     * touches threads the user can see, not the whole inbox regardless of which tab they're on. */
    @Query("UPDATE threads SET unread = 0 WHERE id IN (:ids)")
    suspend fun markThreadsRead(ids: List<String>)

    /** See ThreadEntity.preferredSubscriptionId — only ever called with a non-null subscription
     * id (recordIncomingMessage/reply-send skip this entirely when none is knowable), so an
     * already-known SIM is never overwritten with "unknown" just because one later message
     * happened not to carry the extra. */
    @Query("UPDATE threads SET preferredSubscriptionId = :subscriptionId WHERE id = :id")
    suspend fun setPreferredSubscriptionId(id: String, subscriptionId: Int)

    @Query("DELETE FROM threads WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    /** Called before purgeDeletedBefore so ThreadRepository can gather those threads' messages'
     * systemSmsIds while the rows still exist, to delete-through to the system provider too. */
    @Query("SELECT id FROM threads WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun deletedThreadIdsBefore(cutoff: Long): List<String>

    /** Permanent delete (e.g. "Empty recycle bin") — messages cascade via the foreign key. */
    @Query("DELETE FROM threads WHERE id = :id")
    suspend fun deleteById(id: String)
}

