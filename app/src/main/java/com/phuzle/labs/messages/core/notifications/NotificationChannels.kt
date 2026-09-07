package com.phuzle.labs.messages.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.phuzle.labs.messages.domain.model.NotificationChannelIds

/** Registers the 8 channels so the user gets granular OS-level control. */
object NotificationChannels {

    private data class ChannelSpec(
        val id: String,
        val name: String,
        val description: String,
        val defaultImportance: Int,
        val vibrate: Boolean,
        val sound: Boolean,
        val lockscreenSecret: Boolean = false,
    )

    private val SPECS = listOf(
        ChannelSpec(
            NotificationChannelIds.PERSONAL, "Direct Messages", "Personal conversations",
            NotificationManager.IMPORTANCE_HIGH, vibrate = true, sound = true,
        ),
        ChannelSpec(
            NotificationChannelIds.OTP, "Authentication", "One-time codes and verification messages",
            NotificationManager.IMPORTANCE_HIGH, vibrate = true, sound = true,
        ),
        ChannelSpec(
            NotificationChannelIds.TRANSACTIONS, "Transactions", "Bank and card activity",
            NotificationManager.IMPORTANCE_DEFAULT, vibrate = false, sound = true,
        ),
        ChannelSpec(
            NotificationChannelIds.IPO, "IPO Updates", "Allotment, refund and listing updates",
            NotificationManager.IMPORTANCE_DEFAULT, vibrate = false, sound = true,
        ),
        ChannelSpec(
            NotificationChannelIds.PROMOTIONS, "Promotional", "Offers and marketing messages",
            NotificationManager.IMPORTANCE_LOW, vibrate = false, sound = false, lockscreenSecret = true,
        ),
        ChannelSpec(
            NotificationChannelIds.OTHERS, "Others", "Shipping, appointments, and other automated alerts",
            NotificationManager.IMPORTANCE_DEFAULT, vibrate = false, sound = true,
        ),
        ChannelSpec(
            NotificationChannelIds.UNKNOWN, "Unknown", "Messages we couldn't categorize",
            NotificationManager.IMPORTANCE_LOW, vibrate = false, sound = false, lockscreenSecret = true,
        ),
        ChannelSpec(
            NotificationChannelIds.SYSTEM, "App & Service Alerts", "App updates and service status, pushed from Phuzle",
            NotificationManager.IMPORTANCE_DEFAULT, vibrate = true, sound = true,
        ),
    )

    /** Fresh-install (or app-upgrade) registration, at each channel's shipped default importance.
     * Idempotent for channels that already exist — createNotificationChannel is a no-op on an
     * existing id, it never touches an importance the user has since customized via
     * [updateChannelPriority] or the OS's own per-channel settings page. */
    fun registerAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        SPECS.forEach { spec -> manager.createNotificationChannel(build(spec, spec.defaultImportance)) }
    }

    /** Android only lets an app choose a channel's importance at creation time — calling
     * `NotificationChannel.setImportance` after the fact is a documented no-op on API 26+. The
     * only way to actually change it is to delete the channel and recreate it under the same id,
     * which is what this does when the user picks a new priority in NotificationsSettingsScreen.
     * That reset also drops any OS-level customization (sound, vibration override) the user made
     * for that channel outside the app — same as it would on uninstall/reinstall; there's no API
     * to carry it across a delete. */
    fun updateChannelPriority(context: Context, channelId: String, importance: Int) {
        val spec = SPECS.firstOrNull { it.id == channelId } ?: return
        val manager = NotificationManagerCompat.from(context)
        manager.deleteNotificationChannel(channelId)
        manager.createNotificationChannel(build(spec, importance))
    }

    fun priorityKeyToImportance(priority: String): Int = when (priority) {
        "high" -> NotificationManager.IMPORTANCE_HIGH
        "low" -> NotificationManager.IMPORTANCE_LOW
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }

    fun importanceToPriorityKey(importance: Int): String = when {
        importance >= NotificationManager.IMPORTANCE_HIGH -> "high"
        importance <= NotificationManager.IMPORTANCE_LOW -> "low"
        else -> "default"
    }

    private fun build(spec: ChannelSpec, importance: Int) =
        NotificationChannel(spec.id, spec.name, importance).apply {
            description = spec.description
            enableVibration(spec.vibrate)
            if (spec.sound) setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null) else setSound(null, null)
            if (spec.lockscreenSecret) lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
        }

    /** Deep-links into the system's per-channel settings page, for OS-level control beyond our own toggles. */
    fun channelSettingsIntent(context: Context, channelId: String): android.content.Intent =
        android.content.Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
}
