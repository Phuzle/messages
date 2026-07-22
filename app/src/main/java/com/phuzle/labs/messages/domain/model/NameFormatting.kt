package com.phuzle.labs.messages.domain.model

/** Mirrors the prototype's `initials(name)` helper: '#' for numbers/shortcodes, else first+last initials. */
fun initialsFor(name: String): String {
    if (name.isEmpty()) return "#"
    if (name[0].isDigit() || name[0] == '+') return "#"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "#"
    val first = parts[0].firstOrNull()?.uppercaseChar() ?: return "#"
    val second = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar()
    return if (second != null) "$first$second" else first.toString()
}
