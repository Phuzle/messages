package com.phuzle.labs.messages.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phuzle.labs.messages.data.db.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DraftEntity?

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun delete(id: String)
}
