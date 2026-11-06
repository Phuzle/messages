package com.phuzle.labs.messages.domain.share

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareFormatterTest {
    private val utc = ZoneId.of("UTC")
    private fun ts(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, utc).toInstant().toEpochMilli()

    @Test fun singleReceivedMessage() {
        val out = ShareFormatter.formatMessages("Jordan Reyes", listOf(ShareableMessage(ts(5, 10, 42), false, "are you free tonight?")), utc)
        assertEquals("[5 Nov 2026, 10:42 AM] Jordan Reyes: are you free tonight?", out)
    }

    @Test fun sentMessagesAreAttributedToYou() {
        val out = ShareFormatter.formatMessages("Jordan", listOf(ShareableMessage(ts(5, 22, 5), true, "on my way")), utc)
        assertEquals("[5 Nov 2026, 10:05 PM] You: on my way", out)
    }

    @Test fun multipleMessagesAreChronologicalWhateverTheSelectionOrder() {
        val out = ShareFormatter.formatMessages(
            "Sam",
            listOf(
                ShareableMessage(ts(6, 9, 0), false, "second"),
                ShareableMessage(ts(5, 9, 0), true, "first"),
            ),
            utc,
        )
        assertEquals("[5 Nov 2026, 9:00 AM] You: first\n[6 Nov 2026, 9:00 AM] Sam: second", out)
    }

    @Test fun multiLineBodiesKeepTheirLines() {
        val out = ShareFormatter.formatMessages("Sam", listOf(ShareableMessage(ts(5, 9, 0), false, "line one\nline two\n")), utc)
        assertEquals("[5 Nov 2026, 9:00 AM] Sam: line one\nline two", out)
    }

    @Test fun emptySelectionIsEmpty() = assertEquals("", ShareFormatter.formatMessages("x", emptyList(), utc))

    @Test fun contacts() {
        val out = ShareFormatter.formatContacts(
            listOf(ShareableContact("Jordan Reyes", "+15550100001"), ShareableContact("+18005551001", "+18005551001")),
        )
        assertEquals("Jordan Reyes\n+15550100001\n\n+18005551001", out)
    }
}
