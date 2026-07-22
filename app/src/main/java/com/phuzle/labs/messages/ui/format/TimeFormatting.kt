package com.phuzle.labs.messages.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val zone = ZoneId.systemDefault()
private val timeOfDay = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** Thread-list style: "2m", "1h", "Yesterday", "2d", or a weekday for anything older. */
fun formatThreadListTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diffMinutes = (now - epochMillis) / 60_000
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffMinutes < 60 * 24 && then.toLocalDate() == today -> "${diffMinutes / 60}h"
        then.toLocalDate() == today.minusDays(1) -> "Yesterday"
        diffMinutes < 60 * 24 * 7 -> "${diffMinutes / (60 * 24)}d"
        else -> then.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    }
}

/** In-thread bubble timestamp: "6:24 PM". */
fun formatMessageTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeOfDay)

/** Passbook activity row: "Today, 6:08 PM" / "Yesterday, 9:00 AM" / "Monday, 9:03 AM". */
fun formatTransactionTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val dayLabel = when (then.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> then.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    }
    return "$dayLabel, ${then.format(timeOfDay)}"
}

/** Reminder due label: "today" / "tomorrow" / "in 3 days". */
fun formatDueRelative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val due = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due)
    return when {
        due == today -> "today"
        due == today.plusDays(1) -> "tomorrow"
        days > 0 -> "in $days days"
        else -> "overdue"
    }
}
