package com.phuzle.labs.messages.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The Direct-Messages notification's inline quick-reply target — sends for real via [SmsSender]. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY_TEXT)?.toString()?.trim()
        val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
        val sender = intent.getStringExtra(EXTRA_SENDER)
        if (replyText.isNullOrEmpty() || threadId == null || sender == null) return

        val pendingResult = goAsync()
        val container = context.appContainer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.smsSender.send(sender, replyText)
                container.threadRepository.appendOutgoingMessage(
                    threadId = threadId,
                    body = replyText,
                    scheduledFor = null,
                    scheduleLabel = null,
                    nowMillis = System.currentTimeMillis(),
                )
                container.messageNotifier.confirmReplySent(threadId, sender, replyText)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val KEY_REPLY_TEXT = "key_reply_text"
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_SENDER = "extra_sender"
    }
}
