package com.phuzle.labs.messages.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.phuzle.labs.messages.domain.model.NotificationChannelIds

/** Registers the 4 channels from PRD section 2 so the user gets granular OS-level control. */
object NotificationChannels {

    fun registerAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)

        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelIds.PERSONAL,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Personal conversations"
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelIds.OTP,
                "Authentication",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "One-time codes and verification messages"
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelIds.TRANSACTIONS,
                "Transactions",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Bank and card activity"
                enableVibration(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelIds.PROMOTIONS,
                "Promotional",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Offers, and anything else we couldn't categorize"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelIds.SYSTEM,
                "App & Service Alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "App updates and service status, pushed from Phuzle"
                enableVibration(true)
            }
        )
    }

    /** Deep-links into the system's per-channel settings page, for OS-level control beyond our own toggles. */
    fun channelSettingsIntent(context: Context, channelId: String): android.content.Intent =
        android.content.Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
}
