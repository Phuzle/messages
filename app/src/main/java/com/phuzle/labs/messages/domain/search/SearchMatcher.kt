package com.phuzle.labs.messages.domain.search

/**
 * Substring/word based matching for message and contact search. This replaced a pure subsequence
 * matcher ([FuzzyMatcher]) that treated any text containing the query's letters in order as a hit —
 * "collective" matched "Au Credit Card festive offers" because c…o…l…l…e…c…t…i…v…e can be picked out
 * of it one scattered letter at a time. Here every query token must actually appear in the text, and
 * *how* it appears decides the [Tier]: a whole word beats a word prefix beats a match in the middle
 * of a word. [FuzzyMatcher] is kept only as a last resort for short contact names (e.g. "gdrv").
 */
object SearchMatcher {
    /** More tokens than this are ignored — the SQL pre-filter has a fixed number of slots. */
    const val MAX_TOKENS = 4

    /** Strongest first. Declaration order is the ranking order (see [weight]). */
    enum class Tier(val strong: Boolean) {
        /** The whole text is the query. */
        Exact(true),

        /** The query phrase appears as complete word(s). */
        WholeWord(true),

        /** The query phrase starts at a word boundary ("collect" in "collective offers"). */
        WordPrefix(true),

        /** Every token appears at the start of some word, in any order. */
        TokensAtWord(true),

        /** The query phrase appears, but only inside a word ("tive" in "collective"). */
        PhraseInside(false),

        /** Every token appears somewhere, in any order or position. */
        TokensAnywhere(false),

        /** Scattered letters in order — used for contact names only. */
        Fuzzy(false);

        val weight: Int get() = entries.size - ordinal
    }

    class Query internal constructor(val raw: String, val tokens: List<String>) {
        val phrase: String = tokens.joinToString(" ")
    }

    data class Hit(val tier: Tier, val indices: Set<Int>, val firstIndex: Int)

    /** Null for a blank query. Tokens are whitespace-separated, de-duplicated, capped at [MAX_TOKENS]. */
    fun parse(raw: String): Query? {
        val tokens = raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.distinct().take(MAX_TOKENS)
        if (tokens.isEmpty()) return null
        return Query(raw.trim(), tokens)
    }

    /**
     * @param allowFuzzy permit the scattered-letter fallback (contact names). Message bodies never
     * use it: a long body contains almost any short letter sequence.
     */
    fun match(query: Query, text: String, allowFuzzy: Boolean = false): Hit? {
        if (text.isEmpty()) return null
        val firstOccurrence = IntArray(query.tokens.size) { text.indexOf(query.tokens[it], 0, ignoreCase = true) }
        if (firstOccurrence.any { it < 0 }) return if (allowFuzzy) fuzzy(query, text) else null

        val phraseAt = findPhrase(query.phrase, text)
        val tier = when {
            text.trim().equals(query.phrase, ignoreCase = true) -> Tier.Exact
            phraseAt.any { startsWord(text, it) && endsWord(text, it + query.phrase.length) } -> Tier.WholeWord
            phraseAt.any { startsWord(text, it) } -> Tier.WordPrefix
            query.tokens.all { token -> occurrences(text, token).any { startsWord(text, it) } } -> Tier.TokensAtWord
            phraseAt.isNotEmpty() -> Tier.PhraseInside
            else -> Tier.TokensAnywhere
        }

        val indices = mutableSetOf<Int>()
        for (token in query.tokens) {
            for (at in occurrences(text, token)) for (i in at until at + token.length) indices += i
        }
        return Hit(tier, indices, firstOccurrence.min())
    }

    private fun fuzzy(query: Query, text: String): Hit? {
        // A 1–2 character query is a subsequence of nearly any name; not a meaningful match.
        if (query.raw.length < 3) return null
        val m = FuzzyMatcher.match(query.raw, text) ?: return null
        if (m.quality < FUZZY_MIN_QUALITY) return null
        return Hit(Tier.Fuzzy, m.matchedIndices, m.matchedIndices.minOrNull() ?: 0)
    }

    private fun findPhrase(phrase: String, text: String): List<Int> =
        if (phrase.isEmpty()) emptyList() else occurrences(text, phrase)

    private fun occurrences(text: String, needle: String): List<Int> {
        val found = mutableListOf<Int>()
        var from = 0
        while (found.size < MAX_OCCURRENCES) {
            val at = text.indexOf(needle, from, ignoreCase = true)
            if (at < 0) break
            found += at
            from = at + needle.length.coerceAtLeast(1)
        }
        return found
    }

    private fun startsWord(text: String, index: Int): Boolean = index == 0 || !text[index - 1].isLetterOrDigit()
    private fun endsWord(text: String, end: Int): Boolean = end >= text.length || !text[end].isLetterOrDigit()

    /** A one-line excerpt of [text] with the match in view. [indices] are relative to [text]. */
    data class Snippet(val text: String, val indices: Set<Int>)

    fun snippet(text: String, hit: Hit, maxLength: Int = DEFAULT_SNIPPET_LENGTH): Snippet {
        // Same-length replacement so match indices stay valid, and a multi-line body doesn't lose
        // the line that actually matched to a one-line row's maxLines.
        val flat = buildString(text.length) { for (c in text) append(if (c == '\n' || c == '\r' || c == '\t') ' ' else c) }
        if (flat.length <= maxLength) return Snippet(flat, hit.indices)

        var start = (hit.firstIndex - maxLength / 3).coerceAtLeast(0)
        if (start > 0) {
            // Start on a word boundary when one is close, so the excerpt doesn't open mid-word.
            val boundary = flat.lastIndexOf(' ', start)
            if (boundary >= 0 && start - boundary <= 12) start = boundary + 1
        }
        val end = (start + maxLength).coerceAtMost(flat.length)
        val lead = if (start > 0) "…" else ""
        val tail = if (end < flat.length) "…" else ""
        val shift = lead.length - start
        val indices = hit.indices.filter { it in start until end }.mapTo(mutableSetOf()) { it + shift }
        return Snippet(lead + flat.substring(start, end) + tail, indices)
    }

    /** SQL LIKE patterns for the pre-filter: one per token slot, `%` for unused slots. */
    fun likePatterns(query: Query): List<String> =
        List(MAX_TOKENS) { i -> query.tokens.getOrNull(i)?.let { "%${escapeLike(it)}%" } ?: "%" }

    fun escapeLike(token: String): String =
        token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private const val MAX_OCCURRENCES = 20
    private const val FUZZY_MIN_QUALITY = 40
    const val DEFAULT_SNIPPET_LENGTH = 90
}
