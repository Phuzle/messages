package com.phuzle.labs.messages.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required for the default-SMS-app role to be offered at all, but MMS handling itself is out of
 * scope for this pass (see plan) — incoming MMS are acknowledged and otherwise ignored.
 */
class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Intentionally a no-op: MMS ingestion is not implemented in this pass.
    }
}
