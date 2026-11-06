package com.phuzle.labs.messages.domain.share

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ShareableMessage(val timestamp: Long, val outgoing: Boolean, val text: String)
data class ShareableContact(val name: String, val number: String)

/**
 * Plain-text formats for the system share sheet. Messages follow a chat-export style — one line
 * per message with an unambiguous timestamp and who said it — so a pasted conversation reads
 * the same in any app:
 *
 *     [5 Nov 2026, 10:42 AM] Jordan Reyes: are you free tonight?
 *     [5 Nov 2026, 10:44 AM] You: yes, 8?
 */
object ShareFormatter {
    const val MY_LABEL = "You"

    private val timestampFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.US)

    fun formatMessages(
        otherParty: String,
        messages: List<ShareableMessage>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = messages
        .sortedBy { it.timestamp }
        .joinToString("\n") { m ->
            val stamp = timestampFormat.format(Instant.ofEpochMilli(m.timestamp).atZone(zone))
            "[$stamp] ${if (m.outgoing) MY_LABEL else otherParty}: ${m.text.trim()}"
        }

    /** One block per contact (name, then number), blank line between. A contact whose "name" is
     * just its number is shown once rather than repeated. */
    fun formatContacts(contacts: List<ShareableContact>): String = contacts.joinToString("\n\n") { c ->
        if (c.name.isBlank() || c.name == c.number) c.number else "${c.name}\n${c.number}"
    }
}
