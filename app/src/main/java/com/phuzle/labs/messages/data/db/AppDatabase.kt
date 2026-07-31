package com.phuzle.labs.messages.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phuzle.labs.messages.data.db.dao.BlockedNumberDao
import com.phuzle.labs.messages.data.db.dao.DraftDao
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.PassbookDao
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import com.phuzle.labs.messages.data.db.entity.DraftEntity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ReminderEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.db.entity.TransactionEntity

const val DATABASE_FILE_NAME = "messages.db"

@Database(
    entities = [
        ThreadEntity::class,
        MessageEntity::class,
        BlockedNumberEntity::class,
        TransactionEntity::class,
        ReminderEntity::class,
        DraftEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun passbookDao(): PassbookDao
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** Passbook/Reminders used to be seeded once with the design prototype's sample data
         * (before both became fully SMS-derived); that seeding code is long gone from the source,
         * but any install that ran it already has those rows sitting in its `transactions`/
         * `reminders` tables forever, since nothing else deletes them. This purges them exactly
         * once so real, SMS-derived data isn't mixed in with stale placeholders. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transactions")
                db.execSQL("DELETE FROM reminders")
            }
        }

        /** Adds per-message read state (see MessageEntity.read) so unread counts can reflect real
         * messages instead of just a thread-level flag, and so SmsHistoryImporter can finally
         * carry over each message's actual read/unread state from the system SMS provider instead
         * of importing everything as read. DEFAULT 1 (read) for every pre-existing row: they were
         * already being treated as read before this column existed, so this changes nothing for
         * anyone upgrading, only for messages inserted after. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN read INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** Multi-SIM support (see MessageEntity.subscriptionId / ThreadEntity.preferredSubscriptionId):
         * both columns default to NULL, meaning "no per-SIM info" — exactly how every pre-existing
         * message/thread should read, since none of them have a known subscription. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN subscriptionId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE threads ADD COLUMN preferredSubscriptionId INTEGER DEFAULT NULL")
            }
        }

        /** Correlates a message to its row in the system Telephony.Sms provider (see
         * MessageEntity.systemSmsId) so read-state and delete actions can write through to that
         * shared table, and so regaining the default-SMS-app role can reconcile against deletes/
         * read-changes made by whichever app was default in the meantime — neither direction was
         * synced before this. NULL for every pre-existing row: we don't retroactively know which
         * system row an already-imported message came from, so those simply have nothing to sync
         * against until they're touched again (unaffected — same as never having this column). */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN systemSmsId INTEGER DEFAULT NULL")
            }
        }

        /** Lets the dashboard prefix a thread's preview with "You: " when its last message was
         * sent by us (see ThreadEntity.lastMessageOutgoing). DEFAULT 0 for every pre-existing row
         * is a deliberate, harmless simplification, not a real "we know this was incoming": we
         * don't retroactively know which side sent each thread's current preview, and defaulting
         * to "not outgoing" just means existing threads show unprefixed until their next message
         * updates it either way — never a wrong-but-confident "You:" on someone else's message. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE threads ADD COLUMN lastMessageOutgoing INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Lets a "send later" schedule chosen in Compose survive closing it as a draft (see
         * DraftEntity.scheduledFor) — before this, only the recipient and body were saved, so
         * reopening a scheduled draft looked exactly like an ordinary unscheduled one. NULL for
         * every pre-existing row is correct, not a placeholder: no existing draft has ever had a
         * schedule recorded anywhere, so there is nothing to backfill. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE drafts ADD COLUMN scheduledFor INTEGER DEFAULT NULL")
            }
        }

        /** OTP eviction (see MessageDao.purgeOtpMessagesBefore) used to hard-delete expired OTP
         * messages outright; it now soft-deletes them into the recycle bin like everything else
         * destructive in this app (see MessageEntity.deletedAt), so a code purged early by a false-
         * positive 24h cutoff isn't gone forever. NULL for every pre-existing row is correct: none
         * of them have ever been soft-deleted. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_FILE_NAME,
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    // Deliberately no fallbackToDestructiveMigration(): with real, irreplaceable
                    // user messages in this table, a future version bump that's missing its
                    // Migration must crash loudly (forcing us to write one before shipping) rather
                    // than silently wiping every thread/message/transaction on upgrade — a crash
                    // is fixable with a patch release, a silent wipe is not. Downgrades (e.g.
                    // sideloading an older build over a newer one, a dev-only scenario) are the one
                    // case still allowed to reset, since older code has no way to understand a
                    // newer schema anyway.
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }

        /** Used by [com.phuzle.labs.messages.data.backup.LocalBackupManager] before swapping the db file on restore. */
        fun closeAndReset() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
