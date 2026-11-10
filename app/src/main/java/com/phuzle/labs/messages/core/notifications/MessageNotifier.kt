package com.phuzle.labs.messages.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.phuzle.labs.messages.MainActivity
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.prefs.SettingsRepository
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Builds and posts the real notification for a categorized, just-arrived message: MessagingStyle
 * + inline quick-reply for [Category.Personal], a "Copy Code" clipboard action for [Category.Otp],
 * and a plain notification on the matching channel for everything else.
 */
class MessageNotifier(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** One just-arrived message waiting to be shown. */
    private class Incoming(val thread: ThreadEntity, val message: MessageEntity, val category: Category, val otpCode: String?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queueLock = Mutex()
    private val queue = ArrayList<Incoming>()
    private var batch: CompletableDeferred<Unit>? = null

    /**
     * Messages that arrive in a burst (several SMS_DELIVER broadcasts within moments of each other)
     * used to each post their own notification straight away — all on the same per-thread id, from
     * concurrently running receivers. Android sheds notifications from an app that enqueues more
     * than a handful per second, and replaces rather than stacks posts that share an id, so of five
     * quick messages only one or two ever showed. They are now collected for a short window and
     * posted once per conversation, carrying every message in the burst, a little apart from each
     * other. The caller (the receiver, holding its goAsync result) waits until its batch is posted.
     */
    suspend fun notifyIncoming(thread: ThreadEntity, message: MessageEntity, category: Category, otpCode: String?) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.notificationsAllowed) return
        if (!isChannelEnabled(category, settings)) return
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val done = queueLock.withLock {
            queue += Incoming(thread, message, category, otpCode)
            batch ?: CompletableDeferred<Unit>().also { created ->
                batch = created
                scope.launch {
                    delay(COALESCE_WINDOW_MS)
                    flush()
                }
            }
        }
        done.await()
    }

    private suspend fun flush() {
        val (items, done) = queueLock.withLock {
            val taken = queue.toList()
            queue.clear()
            val finished = batch
            batch = null
            taken to finished
        }
        try {
            val settings = settingsRepository.settingsFlow.first()
            val byThread = items.groupBy { it.thread.id }.values
            byThread.forEachIndexed { index, group ->
                runCatching { post(group, settings) }
                if (index < byThread.size - 1) delay(POST_SPACING_MS)
            }
        } finally {
            done?.complete(Unit)
        }
    }

    private fun post(group: List<Incoming>, settings: com.phuzle.labs.messages.data.prefs.AppSettings) {
        val last = group.last()
        val thread = last.thread
        val builder = when (last.category) {
            Category.Otp -> buildOtpNotification(thread, group, settings.quickActionButtons)
            Category.Personal -> buildPersonalNotification(thread, group, settings.quickActionButtons)
            else -> buildPlainNotification(thread, group, last.category)
        }
        applyLockScreenVisibility(builder, thread, last.category.channelId, settings.lockScreenVisibility)
        manager.notify(thread.id.hashCode(), builder.build())
    }

    /** Clears a thread's notification once it's been read in-app — opening the thread (or a new
     * message arriving while it's already open) marks it read, but "read" and "notification gone"
     * used to be two unrelated things: the notification lingered in the tray with autoCancel only
     * clearing it on an explicit tap, not on reading the conversation by any other route. */
    fun cancelForThread(threadId: String) {
        manager.cancel(threadId.hashCode())
    }

    /** Posted once a scheduled message is actually dispatched (see ScheduledMessageDispatcher,
     * used by both the exact-alarm receiver and the periodic-worker fallback) — the whole point of
     * a schedule is that the user isn't watching when it fires, so without this there was no way
     * to tell "it sent" from "it silently never sent" short of reopening the thread later. Uses
     * the SYSTEM channel (not the recipient's own category channel): this is a status update about
     * the app's own background work, not a new message from that contact, and shouldn't be
     * silenceable by toggling that contact's message-category notifications off. Its own
     * notification id (distinct from thread.id.hashCode(), which is the thread's live-message
     * notification) so this doesn't collide with or get overwritten by a real incoming-message
     * notification for the same thread. */
    fun confirmScheduledSent(thread: ThreadEntity, message: MessageEntity) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, com.phuzle.labs.messages.domain.model.NotificationChannelIds.SYSTEM)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle("Scheduled message sent")
            .setContentText("To ${thread.displayName}: ${message.body}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
            .build()
        manager.notify(("scheduled-sent-" + message.id).hashCode(), notification)
    }

    /** Rebuilds the thread's notification to show the reply we just sent, per RemoteInput convention. */
    fun confirmReplySent(threadId: String, sender: String, replyText: String) {
        val notificationId = threadId.hashCode()
        val me = Person.Builder().setName("You").build()
        val style = NotificationCompat.MessagingStyle(me).addMessage(replyText, System.currentTimeMillis(), me)
        val notification = NotificationCompat.Builder(context, Category.Personal.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setStyle(style)
            .setContentIntent(openThreadIntent(threadId))
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    private fun buildOtpNotification(
        thread: ThreadEntity,
        group: List<Incoming>,
        withActions: Boolean,
    ): NotificationCompat.Builder {
        val latest = group.last()
        val builder = NotificationCompat.Builder(context, Category.Otp.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle(thread.displayName)
            .setContentText(latest.message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(joinedBodies(group)))
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
            .setWhen(latest.message.timestamp)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // The newest code is the one worth one-tap copying.
        val code = group.lastOrNull { it.otpCode != null }?.otpCode
        if (withActions && code != null) {
            val copyIntent = Intent(context, CopyCodeReceiver::class.java).putExtra(CopyCodeReceiver.EXTRA_CODE, code)
            val pendingIntent = PendingIntent.getBroadcast(
                context, thread.id.hashCode(), copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Copy Code", pendingIntent)
        }
        return builder
    }

    private fun buildPersonalNotification(
        thread: ThreadEntity,
        group: List<Incoming>,
        withActions: Boolean,
    ): NotificationCompat.Builder {
        // thread.photoUri (the same one AvatarBubble renders in-app) never made it onto the
        // Person backing this notification, so MessagingStyle always fell back to its default
        // initial-on-color placeholder regardless of whether the contact had a real photo.
        val senderBuilder = Person.Builder().setName(thread.displayName)
        thread.photoUri?.let { uri ->
            runCatching { androidx.core.graphics.drawable.IconCompat.createWithContentUri(uri) }
                .getOrNull()
                ?.let { senderBuilder.setIcon(it) }
        }
        val sender = senderBuilder.build()
        // Carry on the conversation already showing in the tray (if any) rather than starting it
        // over, so a message that arrives a moment after another doesn't erase the first from view.
        val style = existingStyle(thread.id) ?: NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
        group.forEach { style.addMessage(it.message.body, it.message.timestamp, sender) }

        val builder = NotificationCompat.Builder(context, Category.Personal.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setStyle(style)
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
            .setWhen(group.last().message.timestamp)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (withActions) {
            val remoteInput = RemoteInput.Builder(ReplyReceiver.KEY_REPLY_TEXT).setLabel("Reply").build()
            val replyIntent = Intent(context, ReplyReceiver::class.java)
                .putExtra(ReplyReceiver.EXTRA_THREAD_ID, thread.id)
                .putExtra(ReplyReceiver.EXTRA_SENDER, thread.sender)
            val replyPendingIntent = PendingIntent.getBroadcast(
                context, thread.id.hashCode(), replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val replyAction = NotificationCompat.Action.Builder(0, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build()
            builder.addAction(replyAction)
        }
        return builder
    }

    private fun buildPlainNotification(
        thread: ThreadEntity,
        group: List<Incoming>,
        category: Category,
    ): NotificationCompat.Builder {
        val latest = group.last()
        return NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle(thread.displayName)
            .setContentText(latest.message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(withEarlierUnread(thread.id, group)))
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
            .setWhen(latest.message.timestamp)
    }

    /** A thread has one notification slot, so a new arrival replaces the last one. For the styles that
     * can't carry a conversation, keep the earlier bodies that are still in the tray in front of the
     * new ones, so five quick messages read as five rather than just the last. */
    private fun withEarlierUnread(threadId: String, group: List<Incoming>): String {
        val earlier = runCatching {
            context.getSystemService(android.app.NotificationManager::class.java)
                .activeNotifications.firstOrNull { it.id == threadId.hashCode() }
                ?.notification?.extras?.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()
        }.getOrNull()
        val all = (earlier?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()) + group.map { it.message.body }
        return all.takeLast(MAX_PLAIN_LINES).joinToString("\n\n")
    }

    private fun joinedBodies(group: List<Incoming>): String = group.joinToString("\n\n") { it.message.body }

    /** The MessagingStyle of this thread's notification if it's still in the tray. */
    private fun existingStyle(threadId: String): NotificationCompat.MessagingStyle? = runCatching {
        val active = context.getSystemService(android.app.NotificationManager::class.java)
            .activeNotifications.firstOrNull { it.id == threadId.hashCode() }?.notification ?: return null
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(active)
    }.getOrNull()

    private fun applyLockScreenVisibility(
        builder: NotificationCompat.Builder,
        thread: ThreadEntity,
        channelId: String,
        mode: String,
    ) {
        when (mode) {
            "full" -> builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            "hidden" -> builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
            else -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                val publicVersion = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
                    .setContentTitle(thread.displayName)
                    .build()
                builder.setPublicVersion(publicVersion)
            }
        }
    }

    private companion object {
        /** How long a burst of arrivals is gathered before being shown. */
        private const val MAX_PLAIN_LINES = 6
        private const val COALESCE_WINDOW_MS = 400L

        /** Gap between posts for different conversations — Android sheds an app that enqueues more than ~5 a second. */
        const val POST_SPACING_MS = 260L
    }

    private fun isChannelEnabled(category: Category, settings: com.phuzle.labs.messages.data.prefs.AppSettings) =
        when (category.channelId) {
            Category.Personal.channelId -> settings.channelPersonalEnabled
            Category.Otp.channelId -> settings.channelOtpEnabled
            Category.Transactions.channelId -> settings.channelTransactEnabled
            Category.Ipo.channelId -> settings.channelIpoEnabled
            Category.Others.channelId -> settings.channelOthersEnabled
            Category.Unknown.channelId -> settings.channelUnknownEnabled
            else -> settings.channelPromoEnabled
        }

    /** [Intent.ACTION_MAIN] is deliberately not set here — it's unnecessary for an explicit-
     * component intent and, combined with MainActivity's own MAIN/LAUNCHER filter, has caused
     * exactly this kind of "notification tap reopens the app but drops the extras" behavior on
     * some OEM launchers. [android.content.Intent.setData] with a per-thread URI is what actually
     * matters here: extras are *not* part of a PendingIntent's identity for Android's matching
     * purposes, so two thread-open intents that only differ by extra would otherwise collapse
     * into "the same" PendingIntent and only the most recently posted thread's tap would resolve
     * correctly — giving each one distinct request code *and* data keeps them genuinely distinct. */
    private fun openThreadIntent(threadId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(android.net.Uri.parse("app://${context.packageName}/thread/$threadId"))
            .putExtra(MainActivity.EXTRA_OPEN_THREAD_ID, threadId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, threadId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
