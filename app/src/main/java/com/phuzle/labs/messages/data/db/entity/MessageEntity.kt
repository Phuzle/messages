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
    /** Which SIM this message arrived/was sent on, when knowable (see SubscriptionHelper). Null
     * for single-SIM devices, devices/API levels that deny READ_PHONE_STATE, and every message
     * that predates this column — none of those are errors, just "no per-SIM info available". */
    val subscriptionId: Int? = null,
    /** This message's row id in the system Telephony.Sms provider, when the best-effort write
     * there succeeded (see SmsDeliverReceiver/SmsSender/SmsHistoryImporter). Lets read-state and
     * delete actions taken in this app write through to that shared table (SmsProviderSync), and
     * lets AppViewModel.reconcileWithSystemProvider notice deletes/read-changes made by whichever
     * app was default in between — both directions the app previously never synced at all. Null
     * only if that provider write failed, in which case there is nothing to sync against. */
    val systemSmsId: Long? = null,
)
