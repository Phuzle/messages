package com.phuzle.labs.messages.core.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phuzle.labs.messages.BuildConfig
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * adb-only testing hook, never exported to other apps — `android:exported="false"` in the
 * manifest means no third-party app can trigger it, only `adb shell am broadcast` (which targets
 * components directly regardless of the exported flag) or an on-device instrumented test. The
 * [BuildConfig.DEBUG] check is defense in depth on top of that.
 *
 * Exists because [com.phuzle.labs.messages.core.sms.HeadlessSmsSendService] (the OS's "send a
 * quick text reply" hook) only calls [android.telephony.SmsManager] — it never writes to Room —
 * so there was previously no way for a script to simulate a genuine *outgoing* message through the
 * real repository, only inbound ones via `adb emu sms send`.
 *
 * Usage: adb shell am broadcast -n com.phuzle.labs.messages/.core.debug.DebugSimulationReceiver \
 *   -a com.phuzle.labs.messages.debug.SIMULATE_OUTGOING --es number "+15550100001" --es body "On my way!"
 */
class DebugSimulationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        if (intent.action != ACTION_SIMULATE_OUTGOING) return
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

    companion object {
        const val ACTION_SIMULATE_OUTGOING = "com.phuzle.labs.messages.debug.SIMULATE_OUTGOING"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_BODY = "body"
    }
}
