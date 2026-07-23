package com.phuzle.labs.messages.domain.text

/** A phone/URL/email/code found inside a message body, surfaced as an action chip under the bubble. */
data class DetectedEntity(val type: Type, val value: String) {
    enum class Type { Url, Email, Phone, Code }
}

/**
 * Scans a message body for actionable bits — links, email addresses, phone numbers, and
 * verification/reference codes — so the thread screen can offer a Copy/Open button for each
 * instead of making the user select and copy text by hand.
 *
 * "Code" reuses the exact same keyword-plus-pattern heuristic as OTP classification (see
 * RegexRules/CategoryClassifier) rather than a new one: it's already tuned to avoid flagging
 * ordinary numbers (dates, amounts, package weights) as codes, and a message that says "code" or
 * "OTP" near a short digit run is worth a Copy button in any thread, not only ones already
 * classified as Category.Otp.
 */
object MessageEntityDetector {
    private val urlPattern = Regex("""(?i)\b((?:https?://|www\.)[^\s<>"')]+)""")
    private val emailPattern = Regex("""(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""")

    // Deliberately requires 10+ digits (optionally with a leading country code, separated by
    // spaces/dashes/dots/parens) so ordinary short numbers — amounts, dates, reference codes —
    // don't get misflagged as phone numbers.
    private val phonePattern = Regex("""(?<!\d)(\+\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}(?!\d)""")

    fun detect(body: String, otpKeywords: List<String>, otpCodePattern: Regex): List<DetectedEntity> {
        val claimed = mutableListOf<IntRange>()
        val results = mutableListOf<DetectedEntity>()

        fun tryAdd(range: IntRange, type: DetectedEntity.Type, value: String) {
            if (value.isBlank()) return
            if (claimed.any { it.first <= range.last && range.first <= it.last }) return
            claimed += range
            if (results.none { it.type == type && it.value == value }) results += DetectedEntity(type, value)
        }

        urlPattern.findAll(body).forEach {
            tryAdd(it.range, DetectedEntity.Type.Url, it.value.trimEnd('.', ',', ')', '!', '?', ';', ':'))
        }
        emailPattern.findAll(body).forEach { tryAdd(it.range, DetectedEntity.Type.Email, it.value) }
        phonePattern.findAll(body).forEach { tryAdd(it.range, DetectedEntity.Type.Phone, it.value.trim()) }
        if (otpKeywords.any { body.contains(it, ignoreCase = true) }) {
            otpCodePattern.findAll(body).forEach { tryAdd(it.range, DetectedEntity.Type.Code, it.value) }
        }
        return results
    }
}
