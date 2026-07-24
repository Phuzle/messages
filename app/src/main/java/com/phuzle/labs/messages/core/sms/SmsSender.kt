package com.phuzle.labs.messages.core.sms

import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

/** Thin wrapper around [SmsManager] — real sends, since we're the default SMS app. */
class SmsSender(private val context: Context) {

    /** [subscriptionId], when given, sends via that specific SIM (e.g. the SIM a conversation has
     * been happening on — see ThreadEntity.preferredSubscriptionId) instead of whatever the system
     * currently considers the default SMS subscription. A SIM can be removed/deactivated after a
     * thread last remembered it, so resolving it is best-effort: falling back to the plain default
     * manager rather than failing the send outright.
     *
     * Returns the system provider's row id for the sent message (see MessageEntity.systemSmsId),
     * or null if that best-effort provider write failed — callers backfill this onto the Room row
     * they already created (ThreadRepository.setSystemSmsId) so later read/delete actions on this
     * message can write through to the system provider too. */
    fun send(destination: String, body: String, subscriptionId: Int? = null): Long? {
        val manager = resolveManager(subscriptionId)
        val parts = manager.divideMessage(body)
        manager.sendMultipartTextMessage(destination, null, parts, null, null)

        // The default SMS app is responsible for writing its own sent messages into the system
        // provider — Android does not do this automatically. Skipping this means every message
        // sent through this app would exist only in its own private database: invisible to any
        // other app, and gone system-wide the moment this app is uninstalled or replaced as the
        // default handler. Best-effort — a failure here must never block the send itself.
        return runCatching {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, destination)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.READ, 1)
            }
            val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            uri?.let { android.content.ContentUris.parseId(it) }
        }.onFailure { Log.w("SmsSender", "Couldn't record sent message in the system SMS provider", it) }.getOrNull()
    }

    private fun resolveManager(subscriptionId: Int?): SmsManager {
        if (subscriptionId != null) {
            runCatching { SmsManager.getSmsManagerForSubscriptionId(subscriptionId) }.getOrNull()?.let { return it }
        }
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }
}
