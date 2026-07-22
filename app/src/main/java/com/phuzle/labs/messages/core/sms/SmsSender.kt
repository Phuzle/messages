package com.phuzle.labs.messages.core.sms

import android.content.Context
import android.telephony.SmsManager

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
    }
}
