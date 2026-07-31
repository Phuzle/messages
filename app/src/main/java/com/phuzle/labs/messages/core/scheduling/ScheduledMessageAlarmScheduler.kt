package com.phuzle.labs.messages.core.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Fires a scheduled message's send at (as close as the platform allows to) its exact chosen time,
 * instead of relying solely on ScheduledSendWorker's 15-minute WorkManager poll — the reason a
 * message scheduled for, say, 2:03 PM could actually go out anywhere up to 2:15-2:18 PM (WorkManager's
 * own flex/battery-optimization slack stacked on top of the 15-minute period itself). AlarmManager's
 * setExactAndAllowWhileIdle is the platform's own mechanism for "wake up and do this one thing at
 * this one time, even in Doze" — the worker is kept running as a safety net (see
 * ScheduledSendWorker) for whatever this alarm might miss, not replaced by it.
 *
 * Each message gets its own alarm, canceled and re-armed independently as it's created, edited, or
 * deleted — there is no single "next alarm" to juggle, unlike a naive single-alarm-at-a-time design.
 */
class ScheduledMessageAlarmScheduler(private val context: Context) {

    private fun pendingIntent(messageId: Long): PendingIntent {
        val intent = Intent(context, ScheduledSendAlarmReceiver::class.java)
            .setAction(ScheduledSendAlarmReceiver.ACTION_SEND_SCHEDULED)
            // Extras are not part of a PendingIntent's identity for Android's own matching
            // purposes (two otherwise-identical intents differing only in extras collapse into
            // "the same" pending intent) — a distinct request code alone is usually enough, but a
            // per-message data Uri makes that explicit and matches the same defensive pattern
            // already used for notification PendingIntents elsewhere in this app.
            .setData(Uri.parse("app://${context.packageName}/scheduled-message/$messageId"))
            .putExtra(ScheduledSendAlarmReceiver.EXTRA_MESSAGE_ID, messageId)
        return PendingIntent.getBroadcast(
            context, messageId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(messageId: Long, whenMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(messageId)
        // canScheduleExactAlarms() is API 31+ only, and defaults to false on API 33+ for an app
        // like this one that isn't in an exempt category (alarm clocks, calendars, ...) — the user
        // has to explicitly grant "Alarms & reminders" in system settings for the exact path to be
        // available at all. Falling back to setAndAllowWhileIdle (no special permission required)
        // instead of just failing silently still beats the old 15-minute-only poll: it's still a
        // real, single-message alarm, just one Doze is allowed to briefly defer under battery
        // pressure, rather than a periodic sweep that only checks every 15 minutes to begin with.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        }
    }

    fun cancel(messageId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(messageId))
    }
}
