package com.phuzle.labs.messages.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuzle.labs.messages.appContainer

/** Sends compose messages queued with a "send later" schedule option once their time arrives. */
class ScheduledSendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val now = System.currentTimeMillis()
        val due = container.threadRepository.dueScheduledMessages(now)
        for (message in due) {
            val thread = container.threadRepository.getThread(message.threadId) ?: continue
            container.smsSender.send(thread.sender, message.body)
            container.threadRepository.markMessageSent(message.id, now)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "scheduled_send"
    }
}
