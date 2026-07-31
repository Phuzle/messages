package com.phuzle.labs.messages.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuzle.labs.messages.appContainer

/** Fallback safety net for scheduled sends: [com.phuzle.labs.messages.core.scheduling.ScheduledMessageAlarmScheduler]'s
 * exact per-message alarm is the primary dispatch path now, but this periodic sweep still catches
 * anything that alarm missed — the exact-alarm permission not being granted, a device that
 * aggressively kills alarms, or (before this worker existed at all) any other gap. */
class ScheduledSendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        com.phuzle.labs.messages.work.ScheduledMessageDispatcher.dispatchDue(applicationContext.appContainer)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "scheduled_send"
    }
}
