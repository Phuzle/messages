package com.phuzle.labs.messages.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phuzle.labs.messages.appContainer
import com.phuzle.labs.messages.work.ScheduledMessageDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at (as close as the platform allows to) a scheduled message's exact chosen time — see
 * ScheduledMessageAlarmScheduler for why this exists alongside the periodic worker rather than
 * instead of it. [BroadcastReceiver.onReceive] must return quickly, but sending an SMS and writing
 * the result to Room are both suspend work, hence goAsync(): it tells Android "hold off tearing
 * down this receiver's process" for the short-lived coroutine below, which wouldn't otherwise be
 * guaranteed to survive past onReceive returning. */
class ScheduledSendAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ScheduledMessageDispatcher.dispatchOne(context.appContainer, messageId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SEND_SCHEDULED = "com.phuzle.labs.messages.action.SEND_SCHEDULED_MESSAGE"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
    }
}
