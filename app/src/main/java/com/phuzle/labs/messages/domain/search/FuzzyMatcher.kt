package com.phuzle.labs.messages.domain.search

/**
 * A local, in-memory fuzzy matcher (no network, no embeddings) — this is a genuine feature
 * boundary worth stating plainly: true semantic/vector search needs an embedding model, which
 * this project has nowhere to run on-device and no server-side store to keep vectors in. What's
 * here instead is a subsequence matcher in the style of Sublime/VS Code's "fuzzy" file finder:
 * [query]'s characters must all appear in [target], in order, but not necessarily contiguously
 * ("gdrv" matches "Google Drive"). Matches are scored, not just detected, so a search can be
 * ranked and thresholded rather than being a binary yes/no.
 */
object FuzzyMatcher {
    private const val WORD_START_BONUS = 8
    private const val CONSECUTIVE_BONUS = 6
    private const val BASE_SCORE = 1

    /** 0..100 — the match score normalized against the best possible score for a query of this
     * length (an exact, fully-consecutive, word-starting prefix match). Below [MIN_QUALITY] isn't
     * surfaced as a match at all — a handful of scattered, non-consecutive characters technically
     * form a "subsequence" of almost any long string, which would otherwise flood results with
     * matches no human would call a match. */
    const val MIN_QUALITY = 20

    data class Match(val score: Int, val matchedIndices: Set<Int>, val quality: Int)

    fun match(query: String, target: String): Match? {
        val q = query.trim()
        if (q.isEmpty() || target.isEmpty()) return null
        val qLower = q.lowercase()
        val tLower = target.lowercase()

        var qi = 0
        var lastMatchedIndex = -2
        var score = 0
        val indices = mutableSetOf<Int>()
        for (ti in tLower.indices) {
            if (qi >= qLower.length) break
            if (tLower[ti] == qLower[qi]) {
                val isWordStart = ti == 0 || !tLower[ti - 1].isLetterOrDigit()
                val isConsecutive = ti == lastMatchedIndex + 1
                score += BASE_SCORE + (if (isWordStart) WORD_START_BONUS else 0) + (if (isConsecutive) CONSECUTIVE_BONUS else 0)
                indices += ti
                lastMatchedIndex = ti
                qi++
            }
        }
        if (qi < qLower.length) return null // not every query character appears, in order

        val maxPossibleScore = qLower.length * (BASE_SCORE + WORD_START_BONUS + CONSECUTIVE_BONUS)
        val quality = ((score.toDouble() / maxPossibleScore) * 100).toInt().coerceIn(0, 100)
        if (quality < MIN_QUALITY) return null
        return Match(score, indices, quality)
    }
}
