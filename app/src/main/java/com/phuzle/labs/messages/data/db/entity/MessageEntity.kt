package com.phuzle.labs.messages.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("threadId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: String,
    val body: String,
    val timestamp: Long,
    val outgoing: Boolean,
    /** Set when composed with a "send later" schedule option; null means send-now/already-resolved. */
    val scheduledFor: Long? = null,
    val scheduleLabel: String? = null,
    /** For scheduled outgoing messages: true once the send WorkManager job has actually fired. */
    val sent: Boolean = true,
)
