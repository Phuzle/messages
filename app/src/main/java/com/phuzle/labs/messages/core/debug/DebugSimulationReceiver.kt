package com.phuzle.labs.messages.core.debug

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.ContactsContract
import com.phuzle.labs.messages.BuildConfig
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * adb-only testing hook, never exported to other apps — `android:exported="false"` in the
 * manifest means no third-party app can trigger it, only `adb shell am broadcast` (which targets
 * components directly regardless of the exported flag) or an on-device instrumented test. The
 * [BuildConfig.DEBUG] check is defense in depth on top of that.
 *
 * Two actions:
 * - [ACTION_SIMULATE_OUTGOING]: exists because
 *   [com.phuzle.labs.messages.core.sms.HeadlessSmsSendService] (the OS's "send a quick text
 *   reply" hook) only calls [android.telephony.SmsManager] — it never writes to Room — so there
 *   was previously no way for a script to simulate a genuine *outgoing* message through the real
 *   repository, only inbound ones via `adb emu sms send`.
 * - [ACTION_SEED_CONTACT_PHOTO]: `adb shell content insert` (used by scripts/provision_contacts.sh
 *   to create test contacts) has no blob type, so it can only ever create photo-less contacts —
 *   there was no way to test [com.phuzle.labs.messages.core.contacts.ContactLookup.photoUriFor]'s
 *   real code path (a real `content://` contacts photo URI, as opposed to the initials fallback)
 *   from a script. This runs in-process instead, where ContentValues.put(ByteArray) works fine.
 *
 * Usage:
 *   adb shell am broadcast -n com.phuzle.labs.messages/.core.debug.DebugSimulationReceiver \
 *     -a com.phuzle.labs.messages.debug.SIMULATE_OUTGOING --es number "+15550100001" --es body "On my way!"
 *   adb shell am broadcast -n com.phuzle.labs.messages/.core.debug.DebugSimulationReceiver \
 *     -a com.phuzle.labs.messages.debug.SEED_CONTACT_PHOTO --es number "+15550100001" --ei color 0x6C5CE7
 */
class DebugSimulationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return

        when (intent.action) {
            ACTION_SIMULATE_OUTGOING -> {
                val number = intent.getStringExtra(EXTRA_NUMBER) ?: return
                val body = intent.getStringExtra(EXTRA_BODY) ?: return
                val pendingResult = goAsync()
                val container = context.appContainer
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        container.threadRepository.composeOutgoingThread(
                            to = number,
                            body = body,
                            scheduledFor = null,
                            scheduleLabel = null,
                            nowMillis = System.currentTimeMillis(),
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SEED_CONTACT_PHOTO -> {
                val number = intent.getStringExtra(EXTRA_NUMBER) ?: return
                val rgb = intent.getIntExtra(EXTRA_COLOR, 0x6C5CE7)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        seedContactPhoto(context, number, rgb or 0xFF000000.toInt())
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    /** Attaches a solid-color placeholder photo to whichever saved contact resolves for [number] —
     * generated in-process (no bundled asset needed) since there's nothing meaningful to fetch a
     * "real" test avatar from here. Enough to prove the photoUri render path (vs. the initials
     * fallback) actually works end to end. */
    private fun seedContactPhoto(context: Context, number: String, color: Int) {
        val resolver = context.contentResolver
        val lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        var contactId: Long? = null
        resolver.query(lookupUri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) contactId = cursor.getLong(0)
        }
        val cid = contactId ?: return

        var rawContactId: Long? = null
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(cid.toString()),
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) rawContactId = cursor.getLong(0) }
        val rawId = rawContactId ?: return

        val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }

        val values = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
        }
        resolver.insert(ContactsContract.Data.CONTENT_URI, values)
    }

    companion object {
        const val ACTION_SIMULATE_OUTGOING = "com.phuzle.labs.messages.debug.SIMULATE_OUTGOING"
        const val ACTION_SEED_CONTACT_PHOTO = "com.phuzle.labs.messages.debug.SEED_CONTACT_PHOTO"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_BODY = "body"
        const val EXTRA_COLOR = "color"
    }
}
