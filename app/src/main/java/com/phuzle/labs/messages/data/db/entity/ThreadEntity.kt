package com.phuzle.labs.messages.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A conversation with one sender/number. Mirrors the prototype's THREADS_DATA shape. */
@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey val id: String,
    /** Raw address: phone number for real SMS threads, free-text "to" for manually composed ones. */
    val sender: String,
    /** Resolved contact display name, or [sender] verbatim when no contact matches. */
    val displayName: String,
    val category: String,
    val isBusiness: Boolean,
    val avatarColor: Long,
    val lastMessagePreview: String,
    val lastMessageTime: Long,
    val unread: Boolean,
    val archived: Boolean = false,
    val isPrivate: Boolean = false,
    /** null while active; set to the deletion instant once soft-deleted, purged after 30 days. */
    val deletedAt: Long? = null,
)
