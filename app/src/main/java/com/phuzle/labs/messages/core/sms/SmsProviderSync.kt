package com.phuzle.labs.messages.core.sms

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Keeps this app's copy of a message in sync with the system Telephony.Sms provider for the two
 * things that must survive the user switching default SMS apps: read state and deletion. Every
 * call here is keyed off the system provider's own row id (see MessageEntity.systemSmsId) —
 * that id is our own app's bookkeeping, not something a caller should ever need to construct, so
 * every method just takes the ids we already have on hand.
 */
class SmsProviderSync(private val context: Context) {

    fun markRead(systemSmsIds: List<Long>) {
        if (systemSmsIds.isEmpty()) return
        runCatching {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1); put(Telephony.Sms.SEEN, 1) }
            context.contentResolver.update(Telephony.Sms.CONTENT_URI, values, inClause(systemSmsIds), null)
        }.onFailure { Log.w(TAG, "Couldn't mark read in the system SMS provider", it) }
    }

    fun delete(systemSmsIds: List<Long>) {
        if (systemSmsIds.isEmpty()) return
        runCatching {
            context.contentResolver.delete(Telephony.Sms.CONTENT_URI, inClause(systemSmsIds), null)
        }.onFailure { Log.w(TAG, "Couldn't delete from the system SMS provider", it) }
    }

    fun idFromInsertedUri(uri: Uri?): Long? = uri?.let { runCatching { ContentUris.parseId(it) }.getOrNull() }

    /** Every row currently in the system provider, mapped to its read state — used once per
     * regained default-app session to reconcile local state against whatever changed while some
     * other app was default (see AppViewModel.reconcileWithSystemProvider). A single query over
     * the whole table, same cost class as SmsHistoryImporter's one-time backfill scan. */
    fun allRowsSnapshot(): Map<Long, Boolean> {
        val result = mutableMapOf<Long, Boolean>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.READ),
                null, null, null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                while (cursor.moveToNext()) {
                    result[cursor.getLong(idIdx)] = cursor.getInt(readIdx) != 0
                }
            }
        }.onFailure { Log.w(TAG, "Couldn't read the system SMS provider for reconciliation", it) }
        return result
    }

    // ids are our own Longs (Room's autoGenerate rowids echoed back from a prior provider
    // insert), never user-supplied text, so building the IN-list this way is not an injection risk.
    private fun inClause(ids: List<Long>) = "${Telephony.Sms._ID} IN (${ids.joinToString(",")})"

    private companion object {
        const val TAG = "SmsProviderSync"
    }
}
