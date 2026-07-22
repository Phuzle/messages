package com.phuzle.labs.messages.data.repository

import com.phuzle.labs.messages.data.db.dao.DraftDao
import com.phuzle.labs.messages.data.db.entity.DraftEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DraftRepository(private val dao: DraftDao) {
    fun observeAll(): Flow<List<DraftEntity>> = dao.observeAll()
    suspend fun findById(id: String): DraftEntity? = dao.findById(id)
    suspend fun delete(id: String) = dao.delete(id)

    /** Upserts by [id] when editing an existing draft, otherwise creates a fresh one. */
    suspend fun save(id: String?, to: String, body: String): String {
        val draftId = id ?: "draft-" + UUID.randomUUID()
        dao.upsert(DraftEntity(id = draftId, to = to, body = body, updatedAt = System.currentTimeMillis()))
        return draftId
    }
}
