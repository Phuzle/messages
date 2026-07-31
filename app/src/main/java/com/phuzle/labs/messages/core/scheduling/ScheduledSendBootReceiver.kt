package com.phuzle.labs.messages.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager forgets every alarm across a reboot — the database (messages with sent=0 and a
 * scheduledFor) is the only durable record of what was still pending, so this re-arms an exact
 * alarm for each of them the moment the device comes back up. Anything already past its scheduled
 * time by then still gets dispatched correctly: ScheduledMessageAlarmScheduler.schedule with a
 * past whenMillis fires (essentially) immediately rather than silently doing nothing. */
class ScheduledSendBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = context.appContainer
                container.threadRepository.allPendingScheduledMessages().forEach { message ->
                    container.scheduledMessageAlarmScheduler.schedule(message.id, message.scheduledFor!!)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
