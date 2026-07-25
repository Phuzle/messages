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

        // A keyword match still counts (works for the ~60% of messages that arrive in English),
        // but it's OR'd with the structural account/UPI/reference-number signal — bank templates
        // keep those in Latin script and English abbreviations even inside an otherwise Hindi or
        // Punjabi message, so this catches real transactional SMS a keyword-only check would
        // miss for anything sent in a language regex_rules.json doesn't have keywords for yet.
        // The amount match stays mandatory either way, so this never turns "a/c" mentioned in
        // passing into a false Transaction.
        val hasTransactionKeyword = rules.transactionKeywords.any { text.contains(it) }
        val hasAccountRefSignal = rules.accountRefPattern.containsMatchIn(body)
        if ((hasTransactionKeyword || hasAccountRefSignal) && rules.amountPattern.containsMatchIn(body)) {
            return Category.Transactions
        }

        if (rules.promotionKeywords.any { text.contains(it) }) {
            return Category.Promotions
        }

        if (isKnownContact(sender)) {
            return Category.Personal
        }

        if (rules.otherKeywords.any { text.contains(it) }) {
            return Category.Others
        }

        return Category.Unknown
    }
}
