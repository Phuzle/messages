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
    /** Inbound-only in practice (outgoing messages are never "unread"). Defaults true so every
     * pre-existing row from before this column existed, and every message this app itself
     * composes, is trivially correct without a migration having to special-case them. Real
     * incoming messages are explicitly inserted with this false; SmsHistoryImporter instead
     * carries over each message's actual Telephony.Sms.READ value from the system provider. */
    val read: Boolean = true,
)
