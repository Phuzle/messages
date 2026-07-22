package com.phuzle.labs.messages.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.phuzle.labs.messages.data.db.dao.BlockedNumberDao
import com.phuzle.labs.messages.data.db.dao.MessageDao
import com.phuzle.labs.messages.data.db.dao.PassbookDao
import com.phuzle.labs.messages.data.db.dao.ThreadDao
import com.phuzle.labs.messages.data.db.entity.BankAccountEntity
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
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
        BankAccountEntity::class,
        TransactionEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun passbookDao(): PassbookDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_FILE_NAME,
                ).build().also { instance = it }
            }

        /** Used by [com.phuzle.labs.messages.data.backup.LocalBackupManager] before swapping the db file on restore. */
        fun closeAndReset() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
