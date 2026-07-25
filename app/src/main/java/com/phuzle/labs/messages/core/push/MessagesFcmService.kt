package com.phuzle.labs.messages.core.push

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.phuzle.labs.messages.MainActivity
import com.phuzle.labs.messages.domain.model.NotificationChannelIds

/**
 * Receives server-pushed alerts sent from the Firebase console (or, later, a real backend) to the
 * `announcements` topic every install subscribes to in [com.phuzle.labs.messages.MessagesApplication].
 * There's no backend today, so this only handles inbound display — [onNewToken] has nothing to
 * register the token with yet.
 *
 * Expected data payload keys: `type` (`update_available` | `message` | `maintenance` | anything
 * else = generic), `title`, `body`, and for `message`, `threadId` — the same deep-link mechanism
 * MessageNotifier uses for local SMS notifications (see MainActivity.EXTRA_OPEN_THREAD_ID), so a
 * future server-relayed message notification opens straight to that conversation exactly like a
 * locally-received one does, not just the dashboard. Notification-only messages (no `data`) are
 * shown as-is by the system when the app is backgrounded; this only fires for foregrounded
 * delivery or data messages.
 */
class MessagesFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: "Phuzle Messages"
        val body = message.data["body"] ?: message.notification?.body ?: return
        val type = message.data["type"] ?: "generic"
        val threadId = message.data["threadId"]

        val contentIntent = when {
            type == "update_available" -> playStorePendingIntent()
            type == "message" && threadId != null -> openThreadPendingIntent(threadId)
            else -> openAppPendingIntent()
        }

        val notification = NotificationCompat.Builder(this, NotificationChannelIds.SYSTEM)
            .setSmallIcon(com.phuzle.labs.messages.R.drawable.ic_stat_message)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // threadId (when present) keyed instead of type — otherwise every "message" push would
        // collapse onto the same notification id and each new one would just replace the last
        // rather than stacking per-conversation, unlike locally-received SMS notifications.
        NotificationManagerCompat.from(this).notify((threadId ?: type).hashCode(), notification)
    }

    override fun onNewToken(token: String) {
        // No backend to register this with yet — the console's topic-based "announcements"
        // send (see MessagesApplication) doesn't need per-device token bookkeeping on our side.
    }

    private fun playStorePendingIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            setPackage("com.android.vending")
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /** Same shape as MessageNotifier.openThreadIntent (distinct request code + data URI per
     * thread, since extras alone don't make two PendingIntents distinct to Android). */
    private fun openThreadPendingIntent(threadId: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setData(Uri.parse("app://$packageName/thread/$threadId"))
            .putExtra(MainActivity.EXTRA_OPEN_THREAD_ID, threadId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, threadId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
