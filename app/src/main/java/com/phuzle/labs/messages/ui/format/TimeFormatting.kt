package com.phuzle.labs.messages.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val zone = ZoneId.systemDefault()
private val timeOfDay = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val dayAndMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM ''yy", Locale.US)

/**
 * Thread-list style — unambiguous, not relative-count-based: today shows a clock time ("1:09 AM"),
 * yesterday says "Yesterday", anything else this year is a date ("21 Jul"), and anything from a
 * previous year also carries the year ("31 Dec '24"). No bare "4d"/"Monday" that could mean either
 * "4 days ago" or "next Monday" depending on when you read it.
 */
fun formatThreadListTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val thenDate = then.toLocalDate()
    return when {
        thenDate == today -> then.format(timeOfDay)
        thenDate == today.minusDays(1) -> "Yesterday"
        thenDate.year == today.year -> then.format(dayAndMonth)
        else -> then.format(dayMonthYear)
    }
}

/** In-thread bubble timestamp: "6:24 PM". */
fun formatMessageTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeOfDay)

/** Passbook activity row: "Today, 6:08 PM" / "Yesterday, 9:00 AM" / "21 Jul, 9:03 AM" / "31 Dec '24, 9:03 AM". */
fun formatTransactionTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val thenDate = then.toLocalDate()
    val dayLabel = when {
        thenDate == today -> "Today"
        thenDate == today.minusDays(1) -> "Yesterday"
        thenDate.year == today.year -> then.format(dayAndMonth)
        else -> then.format(dayMonthYear)
    }
    return "$dayLabel, ${then.format(timeOfDay)}"
}

/** Compose custom-schedule label: "Today, 3:00 PM" / "Tomorrow, 9:00 AM" / "25 Jul, 9:00 AM". */
fun formatScheduleTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val thenDate = then.toLocalDate()
    val dayLabel = when {
        thenDate == today -> "Today"
        thenDate == today.plusDays(1) -> "Tomorrow"
        thenDate.year == today.year -> then.format(dayAndMonth)
        else -> then.format(dayMonthYear)
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
