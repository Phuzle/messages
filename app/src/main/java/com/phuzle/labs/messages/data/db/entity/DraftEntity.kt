package com.phuzle.labs.messages.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An unsent compose draft, saved when Compose is closed with non-empty, unsent text. */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val to: String,
    val body: String,
    val updatedAt: Long,
)
