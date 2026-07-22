package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phuzle.labs.messages.data.db.entity.BlockedNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun block(entity: BlockedNumberEntity)

    @Delete
    suspend fun unblock(entity: BlockedNumberEntity)

    @Query("SELECT * FROM blocked_numbers ORDER BY number ASC")
    fun observeAll(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean
}
