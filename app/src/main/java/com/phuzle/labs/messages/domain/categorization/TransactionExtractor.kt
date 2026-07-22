package com.phuzle.labs.messages.domain.categorization

/**
 * Best-effort structured facts pulled out of a message body already classified as
 * [com.phuzle.labs.messages.domain.model.Category.Transactions] — real Layer-1 regex extraction,
 * not fabricated. Merchant/last-4 fall back to sensible defaults when the wording doesn't match
 * the common phrasing (e.g. "at Corner Cafe", "ending 8823"); those are heuristics, not guarantees.
 */
data class ExtractedTransaction(
    val merchant: String,
    val accountLast4: String,
    val amountCents: Long,
    val isCredit: Boolean,
)

object TransactionExtractor {
    private val merchantPattern = Regex("\\b(?:at|for) ([A-Z][\\w&'.\\- ]{1,40}?)(?=[,.]|\\s+(?:using|on|via|card|account|ending)\\b|$)")
    private val last4Pattern = Regex("ending(?: in)? (\\d{4})", RegexOption.IGNORE_CASE)
    private val creditKeywords = listOf("credited", "deposited")

    fun extract(body: String, amountPattern: Regex, fallbackMerchant: String): ExtractedTransaction? {
        val amountMatch = amountPattern.find(body) ?: return null
        val amountCents = amountMatch.value
            .filter { it.isDigit() || it == '.' }
            .toDoubleOrNull()
            ?.let { Math.round(it * 100) }
            ?: return null

        val isCredit = creditKeywords.any { body.contains(it, ignoreCase = true) }
        val merchant = merchantPattern.find(body)?.groupValues?.get(1)?.trim() ?: fallbackMerchant
        val last4 = last4Pattern.find(body)?.groupValues?.get(1) ?: ""

        return ExtractedTransaction(
            merchant = merchant,
            accountLast4 = last4,
            amountCents = if (isCredit) amountCents else -amountCents,
            isCredit = isCredit,
        )
    }
}
