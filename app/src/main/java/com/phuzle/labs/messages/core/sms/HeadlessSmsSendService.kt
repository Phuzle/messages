package com.phuzle.labs.messages.core.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.phuzle.labs.messages.appContainer

/**
 * Required for the default-SMS-app role: lets other apps (e.g. "decline call with text") ask us
 * to send a message without opening any UI.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val destination = intent?.data?.schemeSpecificPart
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!destination.isNullOrEmpty() && !body.isNullOrEmpty()) {
            appContainer.smsSender.send(destination, body)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
