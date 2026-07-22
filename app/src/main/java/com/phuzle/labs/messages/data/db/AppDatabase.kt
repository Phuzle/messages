package com.phuzle.labs.messages.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 3,
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

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_FILE_NAME,
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }

        /** Used by [com.phuzle.labs.messages.data.backup.LocalBackupManager] before swapping the db file on restore. */
        fun closeAndReset() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
