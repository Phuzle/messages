package com.phuzle.labs.messages.core.cloud

/**
 * Redacts what the Layer 3 cloud fallback (server/) should never see, before a message body ever
 * leaves the device: account/card/OTP numbers (any run of 4+ digits — long enough to be sensitive,
 * short enough that day-of-month/time fragments like "10am" or "10:30" survive so the reminder
 * extractor upstream can still parse them) and email addresses.
 */
object PiiScrubber {
    private val longDigitRun = Regex("\\d{4,}")
    private val email = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+")

    fun scrub(text: String): String = text
        .replace(email, "[redacted]")
        .replace(longDigitRun, "[redacted]")
}
