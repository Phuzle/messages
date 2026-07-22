package com.phuzle.labs.messages.domain.categorization

import com.phuzle.labs.messages.domain.model.Category

/**
 * Layer 1 of the PRD's multi-layer pipeline: instantaneous, synchronous, on-device regex
 * verification. Layer 2 (Gemini Nano/AICore) and Layer 3 (PII-scrubbed cloud fallback) are not
 * implemented in this pass — ambiguous messages fall through to [Category.Unknown] (or
 * [Category.Personal] when the sender is a known contact) instead of escalating further.
 */
class CategoryClassifier(
    private val rules: RegexRules,
    private val isKnownContact: (String) -> Boolean,
) {
    fun classify(sender: String, body: String): Category {
        val text = body.lowercase()

        val hasOtpKeyword = rules.otpKeywords.any { text.contains(it) }
        if (hasOtpKeyword && rules.otpCodePattern.containsMatchIn(body)) {
            return Category.Otp
        }

        val hasTransactionKeyword = rules.transactionKeywords.any { text.contains(it) }
        if (hasTransactionKeyword && rules.amountPattern.containsMatchIn(body)) {
            return Category.Transactions
        }

        if (rules.promotionKeywords.any { text.contains(it) }) {
            return Category.Promotions
        }

        return if (isKnownContact(sender)) Category.Personal else Category.Unknown
    }
}
