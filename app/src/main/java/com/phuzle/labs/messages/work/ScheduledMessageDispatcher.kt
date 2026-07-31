package com.phuzle.labs.messages.work

import com.phuzle.labs.messages.AppContainer
import com.phuzle.labs.messages.data.db.entity.MessageEntity

/**
 * The actual "send this scheduled message now" logic — shared by [ScheduledSendWorker] (the
 * 15-minute fallback poll) and [com.phuzle.labs.messages.core.scheduling.ScheduledSendAlarmReceiver]
 * (the exact-alarm primary path), so a message dispatched by either route gets identical
 * behavior: sent through the same SIM it was scheduled on, marked sent, linked to its system
 * provider row, and confirmed with the same notification (see MessageNotifier.confirmScheduledSent).
 */
object ScheduledMessageDispatcher {

    /** Dispatches every currently-due scheduled message — the worker's normal sweep. */
    suspend fun dispatchDue(container: AppContainer, now: Long = System.currentTimeMillis()) {
        container.threadRepository.dueScheduledMessages(now).forEach { dispatch(container, it, now) }
    }

    /** Dispatches exactly one message by id, if it's still pending — the alarm receiver's path.
     * Re-checks `!sent` itself: an alarm can still fire after its message was already sent by the
     * fallback worker (or edited/deleted), and this must be a safe no-op in that case, not a
     * duplicate send. */
    suspend fun dispatchOne(container: AppContainer, messageId: Long, now: Long = System.currentTimeMillis()) {
        val message = container.threadRepository.getMessage(messageId) ?: return
        if (message.sent || message.scheduledFor == null || message.scheduledFor > now) return
        dispatch(container, message, now)
    }

    private suspend fun dispatch(container: AppContainer, message: MessageEntity, now: Long) {
        val thread = container.threadRepository.getThread(message.threadId) ?: return
        val systemSmsId = container.smsSender.send(thread.sender, message.body, message.subscriptionId)
        container.threadRepository.markMessageSent(message.id, now)
        if (systemSmsId != null) container.threadRepository.setSystemSmsId(message.id, systemSmsId)
        container.messageNotifier.confirmScheduledSent(thread, message)
    }
}
