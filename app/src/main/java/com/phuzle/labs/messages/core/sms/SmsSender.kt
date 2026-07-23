package com.phuzle.labs.messages.core.sms

import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

/** Thin wrapper around [SmsManager] — real sends, since we're the default SMS app. */
class SmsSender(private val context: Context) {

    fun send(destination: String, body: String) {
        val manager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val parts = manager.divideMessage(body)
        manager.sendMultipartTextMessage(destination, null, parts, null, null)

        // The default SMS app is responsible for writing its own sent messages into the system
        // provider — Android does not do this automatically. Skipping this means every message
        // sent through this app would exist only in its own private database: invisible to any
        // other app, and gone system-wide the moment this app is uninstalled or replaced as the
        // default handler. Best-effort — a failure here must never block the send itself.
        runCatching {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, destination)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.READ, 1)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        }.onFailure { Log.w("SmsSender", "Couldn't record sent message in the system SMS provider", it) }
    }
}
