package com.phuzle.labs.messages.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import com.phuzle.labs.messages.data.db.entity.DraftEntity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Merges a downloaded Google Drive snapshot into the live database, instead of blindly overwriting
 * it (see LocalBackupManager.writeOverLiveDatabase for the destructive "Restore" path used for
 * local backups). This is what makes "restore from Drive" safe to offer even when the device
 * already has real local messages on it — e.g. the first-launch prompt.
 *
 * Threads are matched by [ThreadEntity.sender] (the natural key — a UUID primary key generated on
 * a *different* install has no relationship to this one). Messages don't have a natural key at
 * all (autoGenerate Long ids from two independent installs are essentially guaranteed to collide
 * in range without meaning the same row), so they're de-duplicated by (threadId, timestamp, body,
 * outgoing) instead — two messages with all four the same are the same message. Blocked
 * numbers/drafts/transactions/reminders all carry their own stable string ids already, so those
 * are unioned by id directly. Settings/preferences are never touched by a merge — only message
 * data — since merging two devices' *preferences* has no obviously-correct answer.
 */
class DriveBackupMerger(private val context: Context, private val database: AppDatabase) {

    suspend fun merge(rawSqliteBytes: ByteArray) = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("drive_restore", ".db", context.cacheDir)
        tempFile.writeBytes(rawSqliteBytes)
        val driveDb = SQLiteDatabase.openDatabase(tempFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            mergeThreadsAndMessages(driveDb)
            mergeBlockedNumbers(driveDb)
            mergeDrafts(driveDb)
            mergeTransactions(driveDb)
            mergeReminders(driveDb)
        } finally {
            driveDb.close()
            tempFile.delete()
        }
    }

    private suspend fun mergeThreadsAndMessages(driveDb: SQLiteDatabase) {
        val threadDao = database.threadDao()
        val messageDao = database.messageDao()

        driveDb.rawQuery("SELECT * FROM threads", null).use { threads ->
            while (threads.moveToNext()) {
                val driveThreadId = threads.getString(threads.getColumnIndexOrThrow("id"))
                val sender = threads.getString(threads.getColumnIndexOrThrow("sender"))
                val existingLocal = threadDao.findBySender(sender)

                val localThreadId = if (existingLocal != null) {
                    existingLocal.id
                } else {
                    // No local thread for this sender yet — recreate it from the Drive row,
                    // reusing its id (safe: nothing local references that id yet).
                    threadDao.upsert(
                        ThreadEntity(
                            id = driveThreadId,
                            sender = sender,
                            displayName = threads.getString(threads.getColumnIndexOrThrow("displayName")),
                            category = threads.getString(threads.getColumnIndexOrThrow("category")),
                            isBusiness = threads.getInt(threads.getColumnIndexOrThrow("isBusiness")) != 0,
                            avatarColor = threads.getLong(threads.getColumnIndexOrThrow("avatarColor")),
                            photoUri = threads.getStringOrNull("photoUri"),
                            lastMessagePreview = threads.getString(threads.getColumnIndexOrThrow("lastMessagePreview")),
                            lastMessageTime = threads.getLong(threads.getColumnIndexOrThrow("lastMessageTime")),
                            unread = threads.getInt(threads.getColumnIndexOrThrow("unread")) != 0,
                            archived = threads.getInt(threads.getColumnIndexOrThrow("archived")) != 0,
                            isPrivate = threads.getInt(threads.getColumnIndexOrThrow("isPrivate")) != 0,
                            deletedAt = threads.getLongOrNull("deletedAt"),
                        ),
                    )
                    driveThreadId
                }

                var newestMergedMessage: MessageEntity? = null
                driveDb.rawQuery("SELECT * FROM messages WHERE threadId = ?", arrayOf(driveThreadId)).use { messages ->
                    while (messages.moveToNext()) {
                        val body = messages.getString(messages.getColumnIndexOrThrow("body"))
                        val timestamp = messages.getLong(messages.getColumnIndexOrThrow("timestamp"))
                        val outgoing = messages.getInt(messages.getColumnIndexOrThrow("outgoing")) != 0
                        if (messageDao.countMatching(localThreadId, body, timestamp, outgoing) > 0) continue // already have it

                        val merged = MessageEntity(
                            threadId = localThreadId,
                            body = body,
                            timestamp = timestamp,
                            outgoing = outgoing,
                            scheduledFor = messages.getLongOrNull("scheduledFor"),
                            scheduleLabel = messages.getStringOrNull("scheduleLabel"),
                            sent = messages.getInt(messages.getColumnIndexOrThrow("sent")) != 0,
                        )
                        messageDao.insert(merged)
                        if (newestMergedMessage == null || timestamp > newestMergedMessage!!.timestamp) newestMergedMessage = merged
                    }
                }
                // If the merge brought in a message newer than the local thread's cached preview,
                // refresh it — same reasoning as ThreadRepository's own refreshLastMessage.
                val current = threadDao.findBySender(sender)
                val newest = newestMergedMessage
                if (current != null && newest != null && newest.timestamp > current.lastMessageTime) {
                    threadDao.touchLastMessage(current.id, newest.body, newest.timestamp)
                }
            }
        }
    }

    private suspend fun mergeBlockedNumbers(driveDb: SQLiteDatabase) {
        val dao = database.blockedNumberDao()
        driveDb.rawQuery("SELECT * FROM blocked_numbers", null).use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow("number"))
                if (!dao.isBlocked(number)) dao.block(BlockedNumberEntity(number))
            }
        }
    }

    private suspend fun mergeDrafts(driveDb: SQLiteDatabase) {
        val dao = database.draftDao()
        driveDb.rawQuery("SELECT * FROM drafts", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                if (dao.findById(id) != null) continue
                dao.upsert(
                    DraftEntity(
                        id = id,
                        to = cursor.getString(cursor.getColumnIndexOrThrow("to")),
                        body = cursor.getString(cursor.getColumnIndexOrThrow("body")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")),
                    ),
                )
            }
        }
    }

    private suspend fun mergeTransactions(driveDb: SQLiteDatabase) {
        val dao = database.passbookDao()
        val existingIds = dao.allTransactionIds()
        driveDb.rawQuery("SELECT * FROM transactions", null).use { cursor ->
            val toInsert = mutableListOf<TransactionEntity>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                if (id in existingIds) continue
                toInsert += TransactionEntity(
                    id = id,
                    merchant = cursor.getString(cursor.getColumnIndexOrThrow("merchant")),
                    accountLast4 = cursor.getString(cursor.getColumnIndexOrThrow("accountLast4")),
                    amountCents = cursor.getLong(cursor.getColumnIndexOrThrow("amountCents")),
                    time = cursor.getLong(cursor.getColumnIndexOrThrow("time")),
                    isCredit = cursor.getInt(cursor.getColumnIndexOrThrow("isCredit")) != 0,
                )
            }
            if (toInsert.isNotEmpty()) dao.insertTransactions(toInsert)
        }
    }

    private suspend fun mergeReminders(driveDb: SQLiteDatabase) {
        val dao = database.passbookDao()
        val existingIds = dao.allReminderIds()
        driveDb.rawQuery("SELECT * FROM reminders", null).use { cursor ->
            val toInsert = mutableListOf<ReminderEntity>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                if (id in existingIds) continue
                toInsert += ReminderEntity(
                    id = id,
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    detail = cursor.getString(cursor.getColumnIndexOrThrow("detail")),
                    dueAt = cursor.getLong(cursor.getColumnIndexOrThrow("dueAt")),
                )
            }
            if (toInsert.isNotEmpty()) dao.insertReminders(toInsert)
        }
    }

    private fun android.database.Cursor.getStringOrNull(column: String): String? {
        val idx = getColumnIndex(column)
        return if (idx < 0 || isNull(idx)) null else getString(idx)
    }

    private fun android.database.Cursor.getLongOrNull(column: String): Long? {
        val idx = getColumnIndex(column)
        return if (idx < 0 || isNull(idx)) null else getLong(idx)
    }
}
