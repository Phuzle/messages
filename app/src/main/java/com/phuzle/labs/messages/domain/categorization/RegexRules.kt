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
) {
    /** Used to pre-fill the OTP notification's "Copy Code" action and the 30s hot-swap modal. */
    fun extractCode(body: String): String? = otpCodePattern.find(body)?.value

    companion object {
        fun loadFrom(context: Context): RegexRules {
            val json = context.assets.open("regex_rules.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val otp = root.getJSONObject("otp")
            val transaction = root.getJSONObject("transaction")
            val promotion = root.getJSONObject("promotion")
            return RegexRules(
                otpKeywords = otp.getJSONArray("keywords").toStringList(),
                otpCodePattern = Regex(otp.getString("codePattern")),
                transactionKeywords = transaction.getJSONArray("keywords").toStringList(),
                amountPattern = Regex(transaction.getString("amountPattern")),
                promotionKeywords = promotion.getJSONArray("keywords").toStringList(),
            )
        }

        private fun org.json.JSONArray.toStringList(): List<String> =
            (0 until length()).map { getString(it) }
    }
}
