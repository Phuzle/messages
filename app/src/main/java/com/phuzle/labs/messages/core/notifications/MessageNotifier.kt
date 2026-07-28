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
import kotlinx.coroutines.flow.first

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

    suspend fun notifyIncoming(thread: ThreadEntity, message: MessageEntity, category: Category, otpCode: String?) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.notificationsAllowed) return
        if (!isChannelEnabled(category, settings)) return
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val notificationId = thread.id.hashCode()
        val builder = when (category) {
            Category.Otp -> buildOtpNotification(thread, message, otpCode, settings.quickActionButtons)
            Category.Personal -> buildPersonalNotification(thread, message, settings.quickActionButtons)
            else -> buildPlainNotification(thread, message, category)
        }
        applyLockScreenVisibility(builder, thread, category.channelId, settings.lockScreenVisibility)
        manager.notify(notificationId, builder.build())
    }

    /** Clears a thread's notification once it's been read in-app — opening the thread (or a new
     * message arriving while it's already open) marks it read, but "read" and "notification gone"
     * used to be two unrelated things: the notification lingered in the tray with autoCancel only
     * clearing it on an explicit tap, not on reading the conversation by any other route. */
    fun cancelForThread(threadId: String) {
        manager.cancel(threadId.hashCode())
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
        message: MessageEntity,
        otpCode: String?,
        withActions: Boolean,
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, Category.Otp.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle(thread.displayName)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (withActions && otpCode != null) {
            val copyIntent = Intent(context, CopyCodeReceiver::class.java).putExtra(CopyCodeReceiver.EXTRA_CODE, otpCode)
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
        message: MessageEntity,
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
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
            .addMessage(message.body, message.timestamp, sender)

        val builder = NotificationCompat.Builder(context, Category.Personal.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setStyle(style)
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)
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
        message: MessageEntity,
        category: Category,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle(thread.displayName)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(openThreadIntent(thread.id))
            .setAutoCancel(true)

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

    private fun isChannelEnabled(category: Category, settings: com.phuzle.labs.messages.data.prefs.AppSettings) =
        when (category.channelId) {
            Category.Personal.channelId -> settings.channelPersonalEnabled
            Category.Otp.channelId -> settings.channelOtpEnabled
            Category.Transactions.channelId -> settings.channelTransactEnabled
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
