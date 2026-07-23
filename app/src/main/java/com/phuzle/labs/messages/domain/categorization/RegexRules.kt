package com.phuzle.labs.messages.domain.categorization

import android.content.Context
import org.json.JSONObject

/**
 * Layer-1 heuristics loaded from assets/regex_rules.json — the client-side half of what the PRD
 * calls the shared regex_rules.json (there is no server workspace yet to actually share it with).
 */
data class RegexRules(
    val otpKeywords: List<String>,
    val otpCodePattern: Regex,
    val transactionKeywords: List<String>,
    val amountPattern: Regex,
    val promotionKeywords: List<String>,
    val otherKeywords: List<String>,
) {
    /** Used to pre-fill the OTP notification's "Copy Code" action and the 30s hot-swap modal. */
    fun extractCode(body: String): String? = otpCodePattern.find(body)?.value

    companion object {
        /** Bump this whenever assets/regex_rules.json or CategoryClassifier's own logic changes
         * in a way that would categorize messages differently — it's compared against
         * AppSettings.appliedClassifierVersion on every app launch (see
         * AppViewModel.reclassifyThreadsIfNeeded) to decide whether every existing thread's
         * category needs recomputing against the new rules. Existing threads otherwise keep
         * whatever category they were assigned on first arrival forever, since nothing else ever
         * re-evaluates it — shipping smarter rules would silently do nothing for senders the app
         * already knows about without this. */
        const val CURRENT_VERSION = 1

        fun loadFrom(context: Context): RegexRules {
            val json = context.assets.open("regex_rules.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val otp = root.getJSONObject("otp")
            val transaction = root.getJSONObject("transaction")
            val promotion = root.getJSONObject("promotion")
            val other = root.getJSONObject("other")
            return RegexRules(
                otpKeywords = otp.getJSONArray("keywords").toStringList(),
                otpCodePattern = Regex(otp.getString("codePattern")),
                transactionKeywords = transaction.getJSONArray("keywords").toStringList(),
                amountPattern = Regex(transaction.getString("amountPattern")),
                promotionKeywords = promotion.getJSONArray("keywords").toStringList(),
                otherKeywords = other.getJSONArray("keywords").toStringList(),
            )
        }

        private fun org.json.JSONArray.toStringList(): List<String> =
            (0 until length()).map { getString(it) }
    }
}
